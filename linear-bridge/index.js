const crypto = require('crypto');

const GITHUB_OWNER = 'rmurali79';
const REPOS = ['flyship-backend', 'flyship-frontend', 'flyship-mobile'];

function verifySignature(rawBody, signatureHeader, secret) {
    if (!signatureHeader) return false;
    const expected = crypto.createHmac('sha256', secret).update(rawBody).digest('hex');
    const expectedBuffer = Buffer.from(expected, 'utf8');
    const actualBuffer = Buffer.from(signatureHeader, 'utf8');
    if (expectedBuffer.length !== actualBuffer.length) return false;
    return crypto.timingSafeEqual(expectedBuffer, actualBuffer);
}

function githubHeaders(token) {
    return {
        Authorization: `Bearer ${token}`,
        Accept: 'application/vnd.github+json',
        'X-GitHub-Api-Version': '2022-11-28',
    };
}

// Finds a branch in `repo` whose name starts with the Linear issue identifier
// (e.g. "FLY-11" matches "fly-11-notifications"), case-insensitive.
async function findMatchingBranch(repo, issueId, token) {
    const prefix = issueId.toLowerCase();
    for (let page = 1; page <= 5; page++) {
        const resp = await fetch(
            `https://api.github.com/repos/${GITHUB_OWNER}/${repo}/branches?per_page=100&page=${page}`,
            { headers: githubHeaders(token) }
        );
        if (!resp.ok) {
            console.error(`Failed to list branches for ${repo}: ${resp.status}`);
            return null;
        }
        const branches = await resp.json();
        const match = branches.find((b) => b.name.toLowerCase().startsWith(prefix));
        if (match) return match.name;
        if (branches.length < 100) return null;
    }
    return null;
}

async function triggerDispatch(repo, branch, issueId, token) {
    const resp = await fetch(`https://api.github.com/repos/${GITHUB_OWNER}/${repo}/dispatches`, {
        method: 'POST',
        headers: { ...githubHeaders(token), 'Content-Type': 'application/json' },
        body: JSON.stringify({
            event_type: 'linear-in-progress',
            client_payload: { branch, issue: issueId },
        }),
    });
    if (!resp.ok) {
        console.error(`Failed to dispatch to ${repo}: ${resp.status} ${await resp.text()}`);
        return false;
    }
    return true;
}

exports.linearWebhookBridge = async (req, res) => {
    if (req.method !== 'POST') {
        res.status(405).send('Method not allowed');
        return;
    }

    const webhookSecret = process.env.LINEAR_WEBHOOK_SECRET;
    const githubToken = process.env.GITHUB_TOKEN;
    if (!webhookSecret || !githubToken) {
        console.error('Missing LINEAR_WEBHOOK_SECRET or GITHUB_TOKEN env var');
        res.status(500).send('Server misconfigured');
        return;
    }

    const signature = req.get('Linear-Signature') || req.get('linear-signature');
    const rawBody = req.rawBody ? req.rawBody.toString('utf8') : JSON.stringify(req.body);

    if (!verifySignature(rawBody, signature, webhookSecret)) {
        console.warn('Rejected webhook: invalid or missing signature');
        res.status(401).send('Invalid signature');
        return;
    }

    const payload = req.body || {};

    if (payload.type !== 'Issue' || payload.action !== 'update') {
        res.status(200).send('Ignored: not an issue update event');
        return;
    }

    const newStateName = payload.data && payload.data.state && payload.data.state.name;
    if (newStateName !== 'In Progress') {
        res.status(200).send(`Ignored: state is "${newStateName}", not "In Progress"`);
        return;
    }

    const issueId = payload.data.identifier;
    if (!issueId) {
        res.status(200).send('Ignored: no issue identifier in payload');
        return;
    }

    console.log(`Issue ${issueId} moved to In Progress — checking for matching branches...`);

    const dispatched = [];
    for (const repo of REPOS) {
        const branch = await findMatchingBranch(repo, issueId, githubToken);
        if (branch) {
            const ok = await triggerDispatch(repo, branch, issueId, githubToken);
            if (ok) dispatched.push({ repo, branch });
        }
    }

    console.log(`Dispatched for ${issueId}:`, JSON.stringify(dispatched));
    res.status(200).json({ issue: issueId, dispatched });
};
