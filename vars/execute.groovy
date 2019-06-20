#!/usr/bin/env groovy

def call(body) {
    def pipelineProject = body.pipelineProject
    def buildDockerImage = body.buildDockerImage
    def buildCommand = body.buildCommand
    def nodeLabel = "pipeline-job-${UUID.randomUUID().toString()}"
    echo "Start project on ${nodeLabel}"

    podTemplate(label: nodeLabel,
            serviceAccount: 'helm',
            containers: [
                    containerTemplate(name: 'build', image: "${buildDockerImage}", ttyEnabled: true, command: 'cat'),
                    containerTemplate(name: 'newman', image: 'postman/newman', ttyEnabled: true, command: 'cat'),
                    containerTemplate(name: 'curl', image: 'tutum/curl', ttyEnabled: true, command: 'cat', envVars: [
                            secretEnvVar(key: 'GITEA_TOKEN', secretName: 'gitea-credentials', secretKey: 'accesstoken'),
                            secretEnvVar(key: 'JENKINS_TOKEN', secretName: 'jenkins-credentials', secretKey: 'accesstoken'),
                    ]),
                    containerTemplate(
                            name: 'docker',
                            image: 'docker',
                            command: 'cat',
                            ttyEnabled: true,
                            envVars: [
                                    secretEnvVar(key: 'REGISTRY_USERNAME', secretName: 'registry-credentials', secretKey: 'username'),
                                    secretEnvVar(key: 'REGISTRY_PASSWORD', secretName: 'registry-credentials', secretKey: 'password'),
                                    secretEnvVar(key: 'REGISTRY_URL', secretName: 'registry-credentials', secretKey: 'url')
                            ]
                    ),
                    containerTemplate(name: 'helm', image: 'alpine/helm:2.11.0', command: 'cat', ttyEnabled: true)
            ],
            volumes: [
                    hostPathVolume(hostPath: '/var/run/docker.sock', mountPath: '/var/run/docker.sock')
            ]) {
        node(nodeLabel) {
            stage('Checkout Project') {
                def repo = checkout scm

                def projectName = GiteaHelper.getProjectName("${repo.GIT_URL}")
                def giteaProjectPath = GiteaHelper.getProjectPath("${repo.GIT_URL}")
                def dockerTag = "${repo.GIT_BRANCH}-${repo.GIT_COMMIT}"
                def stageUrl = "${projectName}-${repo.GIT_BRANCH}.${pipelineProject.getStagingBaseUrl()}"

                container('build') {
                    stage('Build Project') {
                        buildCommand.call()
                    }
                }

                // this is pull request branch, or this is a branch, where one of the additional stages should be deployed,
                // then build docker image
                if (env.CHANGE_BRANCH?.trim() || pipelineProject.shouldBeDeployedOnBranch(repo.GIT_BRANCH)) {
                    container('docker') {
                        stage('Build Docker') {
                            // the secrets are provided as variables via DOCKER IMAGE secret mount
                            new PipelineDockerStep(this, "${giteaProjectPath}", "${dockerTag}").execute()
                        }
                    }
                }

                // this is pull request branch, so we need an ephemeral deployment and a notification
                if (env.CHANGE_BRANCH?.trim()) {
                    container('helm') {
                        stage("[${repo.GIT_BRANCH}] Deploy") {
                            new PipelineHelmStep(this, "${projectName}-${repo.GIT_BRANCH}", "${dockerTag}", "${stageUrl}", "${pipelineProject.getStagingVaultPath()}").install()
                        }
                    }

                    // Write message to pull request that staging environment is available for testing
                    // Create Webhook for later delete of staging environment when merge request gets closed or merged
                    container('curl') {
                        stage('Notify pull request') {
                            // GITEA_TOKEN is part of mounted secret variables
                            GiteaHelper.sendCommentToPullRequest(this, "${env.CHANGE_URL}", '\$GITEA_TOKEN', "Successfully created environment for pull request: https://${stageUrl}")
                        }

                        stage('Create delete Webhook if not already exists') {
                            GiteaHelper.createDeleteWebhook(this, "${env.CHANGE_URL}", "${env.JENKINS_URL}", '\$GITEA_TOKEN', '\$JENKINS_TOKEN')
                        }
                    }

                    if (pipelineProject.hasPostmanTests()) {
                        container('newman') {
                            stage("[${repo.GIT_BRANCH}] Test") {
                                new PipelinePostmanStep(this, stageUrl, pipelineProject.getPostmanPath()).execute()
                            }
                        }
                    }
                }

                // iterate over each additionalStage check if this stage should be executed on this branch and ask for deployment if this is not automated
                for (additionalStage in pipelineProject.getStages()) {

                    if (additionalStage.getBranch() != repo.GIT_BRANCH) {
                        // this additional Stage is not valid on this branch, skip this stage
                        continue;
                    }

                    if (additionalStage.requiresInputRequest()) {
                        try {
                            def deploymentInput =
                                    input message: "Continue deployment to [${additionalStage.getName()}]"
                        } catch (err) {
                            // click on abort does not mean stage is failed
                            currentBuild.result = 'SUCCESS'
                            break
                        }
                    }

                    container('helm') {
                        stage("[${additionalStage.getName()}] Deploy") {
                            new PipelineHelmStep(this, "${projectName}-${additionalStage.getName()}", "${dockerTag}", "${additionalStage.getUrl()}", "${additionalStage.getCertVaultPath()}").install()
                        }
                    }

                    if (pipelineProject.hasPostmanTests()) {
                        container('newman') {
                            stage("[${additionalStage.getName()}] Test") {
                                new PipelinePostmanStep(this, "${additionalStage.getUrl()}", pipelineProject.getPostmanPath()).execute()
                            }
                        }
                    }

                }
            }
        }

    }
}
