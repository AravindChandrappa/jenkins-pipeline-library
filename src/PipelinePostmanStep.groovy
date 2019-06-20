class PipelinePostmanStep {
    private def targetUrl
    private def postmanPath
    private Script script

    public PipelinePostmanStep(script, targetUrl, postmanPath) {
        this.script = script
        this.targetUrl = targetUrl
        this.postmanPath = postmanPath
    }

    public void execute() {
        script.sh("""
            newman run \
            ${postmanPath} \
            --env-var url=https://${targetUrl} \
            -n 2
        """)
    }
}
