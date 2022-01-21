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


import org.codehaus.groovy.runtime.InvokerHelper
import org.gradle.api.GradleException
import org.gradle.api.artifacts.dsl.DependencyHandler

class LibsExtension {

    static final String EXT_NAME = 'libs'

    private final DependencyHandler dependencyHandler

    private final Map<String, Lib> libDefinitions = [:]

    LibsExtension(DependencyHandler dependencyHandler) {
        this.dependencyHandler = dependencyHandler
    }

    def call(Closure configClosure) {
        configClosure = (Closure)configClosure.clone()
        configClosure.delegate = this
        configClosure.resolveStrategy = Closure.OWNER_FIRST
        configClosure.call(this)
    }

    def propertyMissing(String name) {
        if (libDefinitions.containsKey(name)) {
            return libDefinitions[name]
        } else {
            throw new MissingPropertyException(name, getClass())
        }
    }

    def methodMissing(String name, Object argsObject) {
        def args = Arrays.asList(InvokerHelper.asArray(argsObject))
        List<Lib> extending = []
        def lastArg = null
        while (!args.empty) {
            if (args.size() > 1) {
                if (args[0] instanceof Lib) {
                    extending.add((Lib)args[0])
                } else {
                    break
                }
            } else {
                lastArg = args[0]
            }
            args = args.subList(1, args.size())
        }
        Lib lib = new Lib(dependencyHandler)
        libDefinitions.putIfAbsent(name, lib)?.with {
            throw new GradleException("Duplicate library: $name")
        }
        extending.each {lib.extendsFrom(it)}
        if (lastArg instanceof Closure) {
            def closure = (Closure)lastArg.clone()
            closure.delegate = lib
            closure.resolveStrategy = Closure.OWNER_FIRST
            closure.call(lib)
            return lib
        } else {
            InvokerHelper.invokeMethod(lib, 'lib', new Object[]{lastArg})
            return lib
        }
    }
}
