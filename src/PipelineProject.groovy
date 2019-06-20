public class PipelineProject {
    private boolean postmanTests;
    private String stagingBaseUrl;
    private String stagingVaultPath;
    private String postmanPath;

    private ArrayList<PipelineStage> stages;

    public PipelineProject(stagingBaseUrl, stagingVaultPath, stages, postmanTests) {
        this.stagingBaseUrl = stagingBaseUrl;
        this.stagingVaultPath = stagingVaultPath;
        this.stages = stages;
        this.postmanTests = postmanTests;
        this.postmanPath = "postman/app.postman_collection.json"
    }

    public PipelineProject(stagingBaseUrl, stagingVaultPath, stages, postmanTests, postmanPath) {
        this.stagingBaseUrl = stagingBaseUrl;
        this.stagingVaultPath = stagingVaultPath;
        this.stages = stages;
        this.postmanTests = postmanTests;
        this.postmanPath = postmanPath
    }

    public boolean hasPostmanTests() {
        return postmanTests
    }

    public String getPostmanPath() {
        return postmanPath
    }

    public String getStagingBaseUrl() {
        return stagingBaseUrl
    }

    public String getStagingVaultPath() {
        return stagingVaultPath;
    }

    public ArrayList<PipelineStage> getStages() {
        return stages;
    }

    public boolean shouldBeDeployedOnBranch(branch) {
        for (stage in stages) {
            if (stage.getBranch() == branch) {
                return true
            }
        }
        return false
    }
}
