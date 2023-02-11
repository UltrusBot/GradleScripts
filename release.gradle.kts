buildscript {
    repositories {
        maven {
            url = uri("https://plugins.gradle.org/m2/")
        }
    }

    dependencies {
        classpath("com.github.breadmoirai:github-release:2.4.1")
        classpath("com.modrinth.minotaur:Minotaur:2.+")
        classpath("gradle.plugin.com.matthewprenger:CurseGradle:1.4.0")
    }
}

apply<com.matthewprenger.cursegradle.CurseGradlePlugin>()
apply<com.modrinth.minotaur.Minotaur>()
apply<com.github.breadmoirai.githubreleaseplugin.GithubReleasePlugin>()
apply(plugin = "maven-publish")

val mod_version: String by project
val githubRepoName: String by project
val githubTargetBranch: String by project

val minecraftVersions: String by project
val modLoaders: String by project
val modDisplayName: String by project
val releaseFileType: String by project

val modrinthProjectId: String by project
val modrinthEmbeddedProjects: String? by project
val modrinthRequiredProjects: String? by project
val modrinthOptionalProjects: String? by project

val curseforgeProjectId: String by project
val curseforgeEmbeddedProjects: String? by project
val curseforgeRequiredProjects: String? by project
val curseforgeOptionalProjects: String? by project


val testRelease: String? by project
val doTest: Boolean = testRelease == null || testRelease == "true"

val syncBody: String? by project
val doSyncBody: Boolean = syncBody == null || syncBody == "true"



configure<com.github.breadmoirai.githubreleaseplugin.GithubReleaseExtension> {
    if (System.getenv("GH_TOKEN") != null) {
        authorization("Token ${System.getenv("GH_TOKEN")}")
    }
    owner("UltrusBot")
    repo(githubRepoName)
    targetCommitish(githubTargetBranch)
    tagName("v${mod_version}")
    body(File("$rootDir/CHANGELOG.md").readText())
    releaseAssets(listOf(tasks.get("remapJar"), tasks.get("remapSourcesJar")))
    dryRun(doTest)
}

configure<com.modrinth.minotaur.ModrinthExtension> {
    if (System.getenv("MODRINTH_TOKEN") != null) {
        token.set(System.getenv("MODRINTH_TOKEN"))
    }
    projectId.set(modrinthProjectId)
    versionType.set(releaseFileType)
    versionName.set("$modDisplayName v${mod_version}")
    uploadFile.set(tasks.get("remapJar"))
    changelog.set(File("$rootDir/CHANGELOG.md").readText())
    gameVersions.set(minecraftVersions.split(",").map { it.trim() })
    loaders.set(modLoaders.split(",").map { it.trim().toLowerCase() })
    dependencies {
        modrinthEmbeddedProjects?.split(",")?.map { it.trim() }?.forEach { embedded.project(it) }
        modrinthRequiredProjects?.split(",")?.map { it.trim() }?.forEach { required.project(it) }
        modrinthOptionalProjects?.split(",")?.map { it.trim() }?.forEach { optional.project(it) }
    }
    debugMode.set(doTest)
    if (doSyncBody) {
        syncBodyFrom.set(rootProject.file("README.md").readText())
    }
}

configure<com.matthewprenger.cursegradle.CurseExtension> {
    if (System.getenv("CF_API_KEY") != null) {
        apiKey = System.getenv("CF_API_KEY")

    }
    project(closureOf<com.matthewprenger.cursegradle.CurseProject> {
        id = curseforgeProjectId
        changelogType = "markdown"
        changelog = File("$rootDir/CHANGELOG.md").readText()
        releaseType = releaseFileType
        minecraftVersions.split(",").map { it.trim() }.forEach { addGameVersion(it) }
        modLoaders.split(",").map { it.trim() }.forEach { addGameVersion(it) }
        mainArtifact(tasks.get("remapJar"))
        mainArtifact.displayName = "$modDisplayName v${mod_version}"
        relations(closureOf<com.matthewprenger.cursegradle.CurseRelation>{
            curseforgeEmbeddedProjects?.split(",")?.map { it.trim() }?.forEach { embeddedLibrary(it) }
            curseforgeRequiredProjects?.split(",")?.map { it.trim() }?.forEach { requiredDependency(it) }
            curseforgeOptionalProjects?.split(",")?.map { it.trim() }?.forEach { optionalDependency(it) }
        })
        options(closureOf<com.matthewprenger.cursegradle.Options> {
            debug = doTest
        })
    })
}