/*-
 * Copyright 2012,2013 © Meikel Brandmeyer.
 * All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

package clojuresque

import clojuresque.tasks.ClojureScriptCompileTask
import clojuresque.tasks.ClojureScriptGzipTask
import clojuresque.tasks.ClojureScriptSourceSet

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Delete
import org.gradle.api.internal.project.ProjectInternal

import kotka.gradle.utils.FileUtil

public class ClojureScriptBasePlugin implements Plugin<Project> {
    public void apply(Project project) {
        project.apply plugin: "java"
        project.apply plugin: "clojure-common"

        configureConfigurations(project)
        configureRuntime(project)
        configureSourceSets(project)
        configureCompilation(project)
        configureGzip(project)
    }

    private void configureConfigurations(Project project) {
        project.configurations {
            clojureScript {
                transitive = true
                visible = false
                description = "ClojureScript configuration to be used for tooling"
            }
        }
    }

    private void configureRuntime(Project project) {
        def props = Util.properties("scriptoresque-base")

        project.dependencies {
            clojuresque group: "clojuresque",
                name: "scriptoresque-base-runtime",
                version: props.getProperty("clojuresque.scriptoresque-base.version")
        }
    }

    private void configureSourceSets(Project project) {
        ProjectInternal projectInternal = (ProjectInternal)project

        project.sourceSets.all { sourceSet ->
            ClojureScriptSourceSet clojureScriptSourceSet =
                new ClojureScriptSourceSet(sourceSet.name, projectInternal.fileResolver)

            sourceSet.convention.plugins.clojureScript = clojureScriptSourceSet
            sourceSet.clojureScript.srcDirs = [
                FileUtil.file("src", sourceSet.name, "cljs")
            ]
            sourceSet.resources.filter.exclude("**/*.cljs")
            sourceSet.allSource.source(clojureScriptSourceSet.clojureScript)
        }
    }

    private void configureCompilation(Project project) {
        project.sourceSets.all { set ->
            if (set.equals(project.sourceSets.test))
                return

            String taskName = set.getCompileTaskName("clojureScript")
            ClojureScriptCompileTask task = project.task(taskName,
                    type: ClojureScriptCompileTask) {
                optimizations = "advanced"
                target = "none"
                pretty = false
                incremental = true
                delayedDestinationDir = {
                    FileUtil.file(project.buildDir, "javascript", set.name)
                }
                delayedClosureDir = {
                    FileUtil.file(project.buildDir, "gclosure", set.name)
                }
                outputFileName = "${set.name}.js"
                source set.clojureScript
                clojureScriptRoots = set.clojureScript
                delayedClasspath = {
                    project.files(
                        (set.hasProperty("clojure") ? set["clojure"].srcDirs : []),
                        project.configurations.clojureScript,
                        set.compileClasspath
                    )
                }
                description = "Compile the ${set.name} ClojureScript source."
            }
            set.output.dir task

            String cleanTaskName = set.getTaskName("cleanCompile", "clojureScript")
            project.task(cleanTaskName, type: Delete) {
                delete { task.destinationDir }
                delete { task.closureDir }
            }
        }
    }

    private void configureGzip(Project project) {
        project.sourceSets.all { set ->
            if (set.equals(project.sourceSets.test))
                return

            String compileTaskName = set.getCompileTaskName("clojureScript")
            ClojureScriptCompileTask compileTask = project.tasks[compileTaskName]

            String taskName = set.getTaskName("gzip", "clojureScript")
            ClojureScriptGzipTask gzipTask = project.task(taskName,
                    type: ClojureScriptGzipTask) {
                delayedArchiveFile = {
                    FileUtil.file(project.buildDir, "javascript", "${set.name}.js.gz")
                }
                source { compileTask.outputFileBuildable }
                description = "Gzip the ${set.name} ClojureScript compilate."
            }
        }
    }
}
