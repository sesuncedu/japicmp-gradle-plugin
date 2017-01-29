/*
 * Copyright 2003-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.champeau.gradle

import org.gradle.api.Task
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.tasks.Input

class ArtifactJapicmpTask extends JapicmpAbstractTask {
    @Input
    Object baseline

    @Input
    Object to

    private File oldArchive
    private File newArchive

    @Override
    File getOldArchive() {
        if (!oldArchive) {
            def dep = project.dependencies.create(baseline, { it.force = true })
            def configuration = project.configurations.detachedConfiguration(dep)
            def res = configuration.resolvedConfiguration.resolvedArtifacts
            ResolvedArtifact resolvedArtifact = res.find {
                ResolvedArtifact it ->
                    def id = it.moduleVersion.id
                    !it.classifier  && id.group  == dep.group && id.name == dep.name && id.version == dep.version }
            //println "resolvedArt = ${resolvedArtifact.properties}"
            oldArchive = resolvedArtifact.file
            oldClasspath = res.findAll  {
                ResolvedArtifact it ->
                    def id = it.moduleVersion.id
                    !(!it.classifier  && id.group  == dep.group && id.name == dep.name && id.version == dep.version) }*.file
            //println "old class path = ${oldClasspath}}"
        }
        oldArchive
    }

    @Override
    File getNewArchive() {
        if (!newArchive) {
            newArchive = computeDependency(to)
        }
        newArchive
    }

    @Override
    Task configure(Closure closure) {
        return super.configure(closure)
    }

    private File computeDependency(Object module) {
        def oldGroup = project.group
        def result = null
        try {
            project.group = 'virtual_group_for_japicmp'
            // convenience wrapping for single file
            def depModule = module instanceof File ? project.files(module) : module
            def configuration = project.configurations.detachedConfiguration(
                    project.dependencies.create(depModule) {
                        if (hasProperty('transitive')) {
                            transitive = false
                        }
                    }
            )

            result = configuration.singleFile
        } finally {
            project.group = oldGroup
        }
        result
    }
}
