/*
 * Copyright 2019 NAVER Corp.
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

package com.navercorp.pinpoint.loader.plugins.profiler;

import com.navercorp.pinpoint.bootstrap.plugin.ProfilerPlugin;
import com.navercorp.pinpoint.loader.plugins.PinpointPluginLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * TODO Loading all plugins with a single class loader could cause class collisions. 使用单个类加载器加载所有插件可能会导致类冲突。
 * Also, with current implementation, plugins can use dependencies by putting them in the plugin directory too.
 * But it can lead to dependency collision between plugins because they are loaded by a single class loader.
 * 另外，根据当前的实现，插件可以通过将它们放在插件目录中来使用依赖项。
 * 但这可能会导致插件之间的依赖冲突，因为它们是由单个类加载器加载的。
 * <p>
 * How can we prevent this?
 * A ClassLoader per plugin could do it but then we have to create "N of target class loader" x "N of plugin" class loaders.
 * It seems too much. For now, Just leave it as it is.
 * 我们如何防止这种情况？
 * 每个插件使用一个 ClassLoader 可以解决这个问题，但这样我们就需要创建“目标类加载器的数量” x “插件的数量” 个类加载器。
 * 这似乎太多了。目前，暂时保持现状。
 *
 * @author Jongho Moon <jongho.moon@navercorp.com>
 * @author emeroad
 * @author HyunGil Jeong
 */
public class ProfilerPluginLoader implements PinpointPluginLoader<ProfilerPlugin> {

    @Override
    public List<ProfilerPlugin> load(ClassLoader classLoader) {
        System.out.println("my|ProfilerPluginLoader|load classLoader = " + classLoader + ", parent = " + classLoader.getParent());
        List<ProfilerPlugin> profilerPlugins = new ArrayList<>();
        ServiceLoader<ProfilerPlugin> serviceLoader = ServiceLoader.load(ProfilerPlugin.class, classLoader);
        for (ProfilerPlugin profilerPlugin : serviceLoader) {
            profilerPlugins.add(profilerPlugin);
        }
        return profilerPlugins;
    }
}
