import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.powerShell
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.pullRequests
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.ScheduleTrigger
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.schedule
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot

project {
    buildType(ChocolateyGUI)
    buildType(ChocolateyGUISchd)
    buildType(ChocolateyGUIQA)
}

object ChocolateyGUI : BuildType({
    id = AbsoluteId("ChocolateyGUI")
    name = "Chocolatey GUI (Built with Unit Tests)"

    artifactRules = """
        code_drop/MsBuild.log
        code_drop/MSBuild.msi.log
        code_drop/ChocolateyGUI.msi
        code_drop/TestResults/issues-report.html
        code_drop/Packages/**/*.nupkg
    """.trimIndent()

    params {
        param("env.vcsroot.branch", "%vcsroot.branch%")
        param("env.Git_Branch", "%teamcity.build.vcs.branch.ChocolateyGUI_ChocolateyGuiVcsRoot%")
        param("teamcity.git.fetchAllHeads", "true")
        password("env.TRANSIFEX_API_TOKEN", "credentialsJSON:c81283e6-cf59-5c9e-9766-6f465018a295", display = ParameterDisplay.HIDDEN, readOnly = true)
        password("env.GITHUB_PAT", "%system.GitHubPAT%", display = ParameterDisplay.HIDDEN, readOnly = true)
    }

    vcs {
        root(DslContext.settingsRoot)

        branchFilter = """
            +:*
        """.trimIndent()
    }

    steps {
        powerShell {
            name = "Prerequisites"
            scriptMode = script {
                content = """
                    # Install Chocolatey Requirements
                    if ((Get-WindowsFeature -Name NET-Framework-Features).InstallState -ne 'Installed') {
                        Install-WindowsFeature -Name NET-Framework-Features
                    }

                    choco install windows-sdk-7.1 netfx-4.0.3-devpack visualstudio2019buildtools netfx-4.8-devpack --confirm --no-progress
                    exit ${'$'}LastExitCode
                """.trimIndent()
            }
        }

        step {
            name = "Include Signing Keys"
            type = "PrepareSigningEnvironment"
        }

        script {
            name = "Call Cake"
            scriptContent = """
                build.official.bat --verbosity=diagnostic --target=CI --testExecutionType=unit --shouldRunOpenCover=false
            """.trimIndent()
        }
    }

    triggers {
        vcs {
            branchFilter = ""
        }
    }

    features {
        pullRequests {
            provider = github {
                authType = token {
                    token = "%system.GitHubPAT%"
                }
            }
        }
    }
})

object ChocolateyGUISchd : BuildType({
    id = AbsoluteId("ChocolateyGUISchd")
    name = "Chocolatey GUI (Scheduled Integration Testing)"

    artifactRules = """
        code_drop/MsBuild.log
        code_drop/MSBuild.msi.log
        code_drop/ChocolateyGUI.msi
        code_drop/TestResults/issues-report.html
        code_drop/Packages/**/*.nupkg
    """.trimIndent()

    params {
        param("env.vcsroot.branch", "%vcsroot.branch%")
        param("env.Git_Branch", "%teamcity.build.vcs.branch.ChocolateyGUI_ChocolateyGuiVcsRoot%")
        param("teamcity.git.fetchAllHeads", "true")
        password("env.TRANSIFEX_API_TOKEN", "credentialsJSON:c81283e6-cf59-5c9e-9766-6f465018a295", display = ParameterDisplay.HIDDEN, readOnly = true)
        password("env.GITHUB_PAT", "%system.GitHubPAT%", display = ParameterDisplay.HIDDEN, readOnly = true)
    }

    vcs {
        root(DslContext.settingsRoot)

        branchFilter = """
            +:*
        """.trimIndent()
    }

    steps {
        powerShell {
            name = "Prerequisites"
            scriptMode = script {
                content = """
                    # Install Chocolatey Requirements
                    if ((Get-WindowsFeature -Name NET-Framework-Features).InstallState -ne 'Installed') {
                        Install-WindowsFeature -Name NET-Framework-Features
                    }

                    choco install windows-sdk-7.1 netfx-4.0.3-devpack visualstudio2019buildtools netfx-4.8-devpack --confirm --no-progress
                    exit ${'$'}LastExitCode
                """.trimIndent()
            }
        }

        step {
            name = "Include Signing Keys"
            type = "PrepareSigningEnvironment"
        }

        script {
            name = "Call Cake"
            scriptContent = """
                build.official.bat --verbosity=diagnostic --target=CI --testExecutionType=all --shouldRunOpenCover=false --shouldRunAnalyze=false --shouldRunIlMerge=false --shouldObfuscateOutputAssemblies=false --shouldRunChocolatey=false --shouldRunNuGet=false
            """.trimIndent()
        }
    }

    triggers {
        schedule {
            schedulingPolicy = daily {
                hour = 2
                minute = 0
            }
            branchFilter = """
                +:<default>
            """.trimIndent()
            triggerBuild = always()
            withPendingChangesOnly = false
        }
    }
})

object ChocolateyGUIQA : BuildType({
    id = AbsoluteId("ChocolateyGUIQA")
    name = "Chocolatey GUI (SonarQube)"

    artifactRules = """
    """.trimIndent()

    params {
        param("env.vcsroot.branch", "%vcsroot.branch%")
        param("env.Git_Branch", "%teamcity.build.vcs.branch.ChocolateyGUI_ChocolateyGuiVcsRoot%")
        param("env.SONARQUBE_ID", "chocolateygui")
        param("teamcity.git.fetchAllHeads", "true")
        password("env.TRANSIFEX_API_TOKEN", "credentialsJSON:c81283e6-cf59-5c9e-9766-6f465018a295", display = ParameterDisplay.HIDDEN, readOnly = true)
        password("env.GITHUB_PAT", "%system.GitHubPAT%", display = ParameterDisplay.HIDDEN, readOnly = true)
    }

    vcs {
        root(DslContext.settingsRoot)

        branchFilter = """
            +:*
        """.trimIndent()
    }

    steps {
        powerShell {
            name = "Prerequisites"
            scriptMode = script {
                content = """
                    # Install Chocolatey Requirements
                    if ((Get-WindowsFeature -Name NET-Framework-Features).InstallState -ne 'Installed') {
                        Install-WindowsFeature -Name NET-Framework-Features
                    }

                    choco install windows-sdk-7.1 netfx-4.0.3-devpack visualstudio2019buildtools netfx-4.8-devpack --confirm --no-progress
                    exit ${'$'}LastExitCode
                """.trimIndent()
            }
        }

        step {
            name = "Include Signing Keys"
            type = "PrepareSigningEnvironment"
        }

        script {
            name = "Call Cake"
            scriptContent = """
                build.official.bat --verbosity=diagnostic --target=CI --testExecutionType=none --shouldRunAnalyze=false --shouldRunIlMerge=false --shouldObfuscateOutputAssemblies=false --shouldRunChocolatey=false --shouldRunNuGet=false --shouldRunSonarQube=true --shouldRunDependencyCheck=true
            """.trimIndent()
        }
    }

    triggers {
        schedule {
            schedulingPolicy = weekly {
                dayOfWeek = ScheduleTrigger.DAY.Saturday
                hour = 2
                minute = 45
            }
            branchFilter = """
                +:<default>
            """.trimIndent()
            triggerBuild = always()
            withPendingChangesOnly = false
        }
    }
})