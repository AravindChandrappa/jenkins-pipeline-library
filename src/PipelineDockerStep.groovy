class PipelineDockerStep {
    private Script script
    private def giteaProjectPath
    private def dockerTag

    public PipelineDockerStep(script, giteaProjectPath, dockerTag) {
        this.script = script
        this.giteaProjectPath = giteaProjectPath
        this.dockerTag = dockerTag
    }

    public PipelineDockerStep build() {
        script.sh("docker build -t \$REGISTRY_URL/${giteaProjectPath}:${dockerTag} .")
        return this
    }

    public PipelineDockerStep login() {
        // TODO: mask login credentials
        script.sh('docker login \$REGISTRY_URL --username \$REGISTRY_USERNAME --password \$REGISTRY_PASSWORD')
        return this
    }

    public PipelineDockerStep push() {
        script.sh("docker push \$REGISTRY_URL/${giteaProjectPath}:${dockerTag}")
        return this
    }

    public PipelineDockerStep cleanup() {
        script.sh("docker rmi \$REGISTRY_URL/${giteaProjectPath}:${dockerTag}")
        return this
    }

    public void execute() {
        build()
                .login()
                .push()
                .cleanup()
    }


}
