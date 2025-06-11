tasks.register("test") {
    //project("client").tasks.getByName("test")
    project("server").tasks.getByName("test")
}