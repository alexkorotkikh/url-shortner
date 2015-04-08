import AssemblyKeys._

assemblySettings

outputPath in assembly := new File("target/url-shortner-0.0.1-SNAPSHOT-jar-with-dependencies.jar")

mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) =>
  {
    case PathList("redis", "clients", xs @ _*) => MergeStrategy.first
    case x => old(x)
  }
}
