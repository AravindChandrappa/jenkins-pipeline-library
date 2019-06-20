class GiteaHelper {

    private static String extractMatch(url, index) {
        if ("${url}".startsWith("git")) {
            // ssh project
            def matches = (url =~ /.*:(.*?)\/(.*?).git$/)
            if (matches.find()) {
                matches[0][index]
            } else {
                throw new Exception("Can't extract pattern from url: ${url}")
            }
        } else {
            // http project
            def matches = (url =~ /.*?\/\/.*?\/(.*?)\/(.*?).git$/)
            if (matches.find()) {
                matches[0][index]
            } else {
                throw new Exception("Can't extract pattern from url: ${url}")
            }
        }
    }

    public static String getProjectName(url) {
        extractMatch(url, 2)
    }

    public static String getGroupName(url) {
        extractMatch(url, 1)
    }

    public static String getProjectPath(url) {
        "${extractMatch(url, 1)}/${extractMatch(url, 2)}"
    }

    // get from issue url the base domain
    // e.g. https://test.com/development/demo/pulls/11 --> https://test.com
    public static String getGiteaDomain(url) {
        def matches = (url =~ /(http[s]?:\/\/.*?)\//)
        if (matches.find()) {
            matches[0][1]
        } else {
            throw new Exception("Can't extract pattern from url: ${url}")
        }
    }

    // get from issue url the pull request index
    // e.g. https://test.com/development/demo/pulls/11 --> 11
    public static String getPullRequestIndex(url) {
        def slashIndex = url.lastIndexOf('/');
        url.substring(slashIndex + 1)
    }

    public static void sendCommentToPullRequest(script, pullRequestUrl, giteaToken, message) {
        def pullRequestApiUrl = "${getGiteaDomain(pullRequestUrl)}/api/v1/repos/${getProjectPathFromPullRequestUrl(pullRequestUrl)}/issues/${getPullRequestIndex(pullRequestUrl)}/comments?access_token=${giteaToken}"
        script.echo pullRequestApiUrl

        script.sh("curl -X POST \"${pullRequestApiUrl}\" -H  'accept: application/json' -H  'Content-Type: application/json' -d '{ \"body\": \"${message}\"}'")
    }

    public static String getProjectPathFromPullRequestUrl(url) {
        def matches = (url =~ /http[s]?:\/\/.*?\/(.*?\/.*?)\//)
        if (matches.find()) {
            matches[0][1]
        } else {
            throw new Exception("Can't extract pattern from url: ${url}")
        }
    }

    public static void createDeleteWebhook(script, pullRequestUrl, jenkinsUrl, giteaToken, jenkinsToken) {
        def jenkinsTriggerUrl = "${jenkinsUrl}generic-webhook-trigger/invoke"
        def webhookUrl = "${getGiteaDomain(pullRequestUrl)}/api/v1/repos/${getProjectPathFromPullRequestUrl(pullRequestUrl)}/hooks?access_token=${giteaToken}"
        def responseBody = script.sh(returnStdout: true, script: "curl -X GET \"${webhookUrl}\" -H 'accept: application/json'")

        if (!"${responseBody}".contains("${jenkinsTriggerUrl}")) {
            def addWebHookBody = """{
                \\\"type\\\": \\\"gitea\\\",
                \\\"config\\\": {
                    \\\"content_type\\\": \\\"json\\\",
                    \\\"url\\\": \\\"${jenkinsTriggerUrl}?token=${jenkinsToken}\\\"
                },
                \\\"events\\\": [
                    \\\"pull_request\\\"
                ],
                \\\"active\\\": true
            }"""
            script.sh("curl -X POST \"${webhookUrl}\" -H  'accept: application/json' -H  'Content-Type: application/json' -d \"${addWebHookBody}\"")
        } else {
            script.echo('Webhook already exists')
        }
    }

}
