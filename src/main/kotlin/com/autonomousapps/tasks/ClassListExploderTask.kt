// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.internal.ClassFilesParser
import com.autonomousapps.internal.utils.bufferWriteJsonSet
import com.autonomousapps.internal.utils.filterToClassFiles
import com.autonomousapps.internal.utils.getAndDelete
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

@CacheableTask
abstract class ClassListExploderTask @Inject constructor(
  private val workerExecutor: WorkerExecutor,
  private val layout: ProjectLayout,
) : AndroidClassesTask() {

  init {
    description = "Produces a report of all classes referenced by a given set of class files"
  }

  /** Class files generated by any JVM source (Java, Kotlin, Groovy, etc.). May be empty. */
  @get:Classpath
  @get:InputFiles
  abstract val classes: ConfigurableFileCollection

  @get:OutputFile
  abstract val output: RegularFileProperty

  @TaskAction fun action() {
    workerExecutor.noIsolation().submit(ClassListExploderWorkAction::class.java) {
      // JVM projects
      classFiles.setFrom(classes.asFileTree.filterToClassFiles().files)
      // Android projects
      classFiles.from(androidClassFiles())

      buildDir.set(layout.buildDirectory)
      output.set(this@ClassListExploderTask.output)
    }
  }

  interface ClassListExploderParameters : WorkParameters {
    val classFiles: ConfigurableFileCollection
    val buildDir: DirectoryProperty
    val output: RegularFileProperty
  }

  abstract class ClassListExploderWorkAction : WorkAction<ClassListExploderParameters> {

    override fun execute() {
      val output = parameters.output.getAndDelete()

      val usedClasses = ClassFilesParser(
        classes = parameters.classFiles.asFileTree.files,
        buildDir = parameters.buildDir.get().asFile
      ).analyze()

      output.bufferWriteJsonSet(usedClasses)
    }
  }
}
