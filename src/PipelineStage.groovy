public class PipelineStage {

    private String name;
    private String url;
    private String certVaultPath
    private String branch
    private boolean requiresInputRequest

    public PipelineStage(name, url, certVaultPath, branch, requiresInputRequest) {
        this.name = name;
        this.url = url;
        this.certVaultPath = certVaultPath;
        this.branch = branch
        this.requiresInputRequest = requiresInputRequest
    }

    public String getName() {
        return name;
    }

    public String getBranch() {
        return branch
    }

    public String getUrl() {
        return url;
    }

    public String getCertVaultPath() {
        return certVaultPath;
    }

    public boolean requiresInputRequest() {
        return requiresInputRequest
    }

}
