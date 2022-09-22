load("@rules_jvm_external//:defs.bzl", "maven_install")
load("@bazel_tools//tools/build_defs/repo:git.bzl", "git_repository")


def rocksdb_pruebas_deps():
    maven_install(
        artifacts =  [
            "org.rocksdb:rocksdbjni:7.5.3"
        ],
        generate_compat_repositories = True,
        repositories = [
            "https://repo.maven.apache.org/maven2/",
            "https://mvnrepository.com/",
        ],
    )
