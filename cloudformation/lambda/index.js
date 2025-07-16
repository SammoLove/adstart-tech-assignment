const https = require('https');
const { SNS } = require('aws-sdk');

const sns = new SNS();

exports.handler = async () => {
    const apiUrl = process.env.API_URL;
    const topicArn = process.env.SNS_TOPIC_ARN;

    return new Promise((resolve, reject) => {
        https.get(apiUrl, res => {
            let body = '';
            res.on('data', chunk => body += chunk);
            res.on('end', async () => {
                try {
                    const domains = JSON.parse(body);
                    if (!domains.length) return resolve('No expiring domains');

                    const message = `Expiring domains: ${domains.map(d => d.name).join(', ')}`;
                    await sns.publish({ Message: message, TopicArn: topicArn }).promise();
                    resolve('Notification sent');
                } catch (e) {
                    reject(e);
                }
            });
        }).on('error', reject);
    });
};
