repositories {
    maven("https://repo.glaremasters.me/repository/concuncan/")
    maven("https://repo.dmulloy2.net/repository/public/")
}

dependencies {
    api(project(":shared"))
    compileOnly(fileTree("worldedit"))
    compileOnly("com.grinderwolf:slimeworldmanager-plugin:2.2.1")
}
