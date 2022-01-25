/*
 *  Copyright (c) 2022 Raffael Herzog
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to
 *  deal in the Software without restriction, including without limitation the
 *  rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 *  sell copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 *  FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 *  IN THE SOFTWARE.
 */

package local


import org.gradle.api.GradleException
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler

import java.util.regex.Pattern

import static java.util.Objects.requireNonNullElse

class Lib implements List<Dependency> {

    private static final DEP_RE = Pattern.compile(/(?:([^:@]*):)?([^:@]+)(?::([^:@]*))?(.*)?/)

    private final DependencyHandler dependencyHandler
    @Delegate(includeTypes = List)
    private final List<Dependency> dependencies = []

    private String group = null
    private String version = null

    Lib(DependencyHandler dependencyHandler) {
        this.dependencyHandler = dependencyHandler
    }

    Lib group(Object group) {
        setGroup(group as String)
        return this
    }

    Lib version(Object version) {
        setVersion(version as String)
        return this
    }

    void setGroup(String group) {
        if (this.@group != null) {
            throw new IllegalStateException("Cannot set group to '$group', already set to '$this.group'")
        }
        this.@group = group
    }

    void setVersion(String version) {
        if (this.@version != null) {
            throw new IllegalStateException("Cannot set version to '$version', already set to '$this.version'")
        }
        this.@version = version
    }

    Lib extendsFrom(Object that) {
        if (that instanceof Lib) {
            extendsFrom((Lib)that)
        } else {
            throw new IllegalArgumentException("Cannot extend from $that ($that.class)")
        }
    }

    Lib extendsFrom(Lib that) {
        if (group == null && that.group != null) {
            group that.group
        }
        if (version == null && that.version != null) {
            version that.version
        }
        lib that
    }

    Lib lib(Object spec, Closure configureClosure = null) {
        if (spec instanceof Lib) {
            for (s in spec) {
                lib(s, configureClosure)
            }
            return this
        } else if (spec instanceof CharSequence) {
            def m = DEP_RE.matcher((CharSequence)spec)
            if (m.matches()) {
                def g = m.group(1) ?: defaultGroup(spec)
                def n = m.group(2)
                def v = m.group(3) ?: defaultVersion(spec)
                if (group == null) group(g)
                if (version == null) version(v)
                g = resolveRelativeGroup(g)
                def newSpec = "$g:$n:$v${m.group(4)}"
                dependencies.add(dependencyHandler.create(newSpec as String, configureClosure))
                return this
            }
        } else if (spec instanceof Map) {
            def map = new LinkedHashMap((Map)spec)
            def g = spec['group'] as String ?: defaultGroup(spec)
            def v = spec['version'] as String ?: defaultVersion(spec)
            if (group == null) group(g)
            if (version == null) version(v)
            g = resolveRelativeGroup(g)
            if (g) {
                map['group'] = g
            }
            if (v) {
                map['version'] = v
            }
            dependencies.add(dependencyHandler.create(map, configureClosure))
            return this
        }
        dependencies.add(dependencyHandler.create(spec, configureClosure))
        return this
    }

    String defaultGroup(Object source = null) {
        if (group == null) {
            throw new GradleException("No main group set${requireNonNullElse(': ' + source, '')}")
        }
        return group
    }

    String defaultVersion(Object source = null) {
        if (version == null) {
            throw new GradleException("No main version set${requireNonNullElse(': ' + source, '')}")
        }
        return version
    }

    String resolveRelativeGroup(String group) {
        if (!group.startsWith('.')) {
            return group
        }
        def base = defaultGroup(group)
        while (group.startsWith('.')) {
            group = group.substring(1)
        }
        return group ? base+'.'+group : base
    }
}
