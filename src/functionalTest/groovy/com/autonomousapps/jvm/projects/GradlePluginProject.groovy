package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.advice.Advice
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Plugin
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType

final class GradlePluginProject extends AbstractProject {

  final GradleProject gradleProject

  GradlePluginProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withSubproject('plugin') { s ->
      s.sources = sources
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.javaGradlePlugin]
        bs.dependencies = []
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private List<Source> sources = [
    new Source(
      SourceType.JAVA, "MagicPlugin", "com/example",
      """\
        package com.example;
        
        import org.gradle.api.*;

        public class MagicPlugin implements Plugin<Project> {
          @Override
          public void apply(Project project) {}
        }
      """.stripIndent()
    )
  ]

  final List<Advice> expectedAdvice = []
}