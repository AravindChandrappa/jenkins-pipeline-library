class PipelineHelmStep {

    Script script
    private String name
    private String dockerTag
    private String url
    private String vaultPath

    public PipelineHelmStep(script, name, dockerTag, url, vaultPath) {
        this.script = script
        this.name = name
        this.dockerTag = dockerTag
        this.url = url
        this.vaultPath = vaultPath
    }

    def install() {
        script.sh("""helm upgrade --install ${name} --namespace='${name}' \
            --set ingress.enabled=true \
            --set ingress.hosts[0]='${url}' \
            --set ingress.annotations.\"kubernetes\\.io/ingress\\.class\"=nginx \
            --set ingress.tls[0].hosts[0]='${url}' \
            --set ingress.tls[0].secretName='tls-${url}' \
            --set ingress.tls[0].vaultPath='${vaultPath}' \
            --set ingress.tls[0].vaultType='CERT' \
            --set image.tag='${dockerTag}' \
            --wait \
            ./helm""")
    }

}
