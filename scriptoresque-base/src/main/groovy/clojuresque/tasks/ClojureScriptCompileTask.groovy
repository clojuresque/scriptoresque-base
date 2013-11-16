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

package clojuresque.tasks

import kotka.gradle.utils.ConfigureUtil
import kotka.gradle.utils.Delayed
import kotka.gradle.utils.FileUtil

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.StopExecutionException
import org.gradle.api.tasks.TaskAction

import java.io.File

public class ClojureScriptCompileTask extends ClojureScriptSourceTask {
    @Input
    def optimizations

    @Input
    def target

    @Input
    def pretty

    @Input
    def incremental

    @OutputDirectory
    @Delayed
    def destinationDir

    @Delayed
    def closureDir

    @Input
    @Delayed
    def outputFileName

    @InputFiles
    @Delayed
    def classpath

    def clojureScriptRoots
    def jvmOptions = {}

    def getOutputFile() {
        FileUtil.file(getDestinationDir(), getOutputFileName())
    }

    def getOutputFileBuildable() {
        project.files(outputFile).builtBy(this)
    }

    @TaskAction
    public void compile() {
        def destDir = this.getDestinationDir()
        if (destDir == null) {
            throw new StopExecutionException("destinationDir not set!")
        }
        destDir.mkdirs()

        def cDir = this.getClosureDir()
        if (cDir == null) {
            throw new StopExecutionException("closureDir not set!")
        }
        if (!incremental) {
            FileUtil.remove(cDir)
        }
        cDir.mkdirs()

        if (getOutputFileName() == null) {
            throw new StopExecutionException("outputFileName not set!")
        }
        outputFile.parentFile.mkdirs()

        List<String> options = [
            "-i", clojureScriptRoots.srcDirs.collect { it.path }.join(File.pathSeparator),
            "-d", cDir.path,
            "-o", outputFile.path,
            "-O", optimizations,
        ]

        if (!target.equals("none")) {
            options.add("-t")
            options.add(target)
        }

        if (pretty) {
            options.add("-p")
        }

        project.clojureexec {
            ConfigureUtil.configure delegate, this.jvmOptions
            classpath = project.files(
                this.clojureScriptRoots.srcDirs,
                this.classpath
            )
            main = "clojuresque.tasks.compile-cljs/main"
            args = options
        }
    }
}
