/**
 * Danta AEM Bundle
 *
 * Copyright (C) 2017 Tikal Technologies, Inc. All rights reserved.
 *
 * Licensed under GNU Affero General Public License, Version v3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.gnu.org/licenses/agpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied;
 * without even the implied warranty of MERCHANTABILITY.
 * See the License for more details.
 */

package danta.aem.contextprocessors;

import com.google.common.collect.Sets;
import danta.aem.templating.TemplateContentModelImpl;
import danta.api.ExecutionContext;
import danta.api.configuration.Configuration;
import danta.api.configuration.Mode;
import danta.api.exceptions.ProcessException;
import danta.core.contextprocessors.AbstractCheckComponentCategoryContextProcessor;
import net.minidev.json.JSONValue;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;

import java.util.Collection;
import java.util.Set;

import static danta.Constants.COMPONENT_CATEGORY;
import static danta.Constants.LOW_PRIORITY;
import static danta.aem.Constants.SLING_HTTP_REQUEST;
import static danta.core.Constants.XK_DESERIALIZE_JSON_PROPS_CP;

/**
 * This context processor is intended to work with widgets that store
 * the data into a JCR string property using the Json format like the "Multi
 * Field Panel" in ACS Commons. The context processor takes the property
 * and deserializes the information to make it available for handlebars
 * as multiple objects, instead of sending a single string with all
 * the data.
 * <p>
 * For example, the widget is going to store into a single property:
 * <pre><blockquote>
 *     {"title":"about blah","text":"Lorem ipsum"}
 * </blockquote></pre>
 * <p>
 * This context processor takes that strings and replaces it with two
 * objects named: "title", and "text".
 * <p>
 * This can be enabled by adding a multi-value string property named "xk_deserializeJSON"
 * to the "xk.config" node in the component, with the name of all the properties that
 * must be deserialized, for example:
 * <p>
 * <pre><blockquote>
 *     Property name: xk_deserializeJSON, Type: String[],  Value: content.comments
 * </blockquote></pre>
 * <p>
 *
 * xk_deserializeJSON    Multi-value string property with the name of all the properties that
 *                       must be deserialized (it must be added to the xk.config node).
 *
 * @see         <a href="http://git.io/52DUXw">ACS Commons Multifield Panel</a>
 * @author      joshuaoransky
 * @version     1.0.0
 * @since       2014-09-04
 */
@Component
@Service
public class DeserializeJSONPropertyValuesContextProcessor
        extends AbstractCheckComponentCategoryContextProcessor<TemplateContentModelImpl> {

    @Override
    public Set<String> anyOf() {
        return Sets.newHashSet(COMPONENT_CATEGORY);
    }

    @Override
    public int priority() {
        return LOW_PRIORITY;
    }

    @Override
    public void process(final ExecutionContext executionContext, final TemplateContentModelImpl contentModel)
            throws ProcessException {
        try {
            SlingHttpServletRequest request = (SlingHttpServletRequest) executionContext.get(SLING_HTTP_REQUEST);
            Configuration config = configurationProvider.getFor(request.getResource().getResourceType());
            Collection<String> propsWithJSONValues = config.asStrings(XK_DESERIALIZE_JSON_PROPS_CP, Mode.MERGE);

            for(String propName : propsWithJSONValues) {
                if(contentModel.has(propName)) {
                    String jsonString = contentModel.getAsString(propName);
                    if(JSONValue.isValidJson(jsonString)) {
                        Object value = JSONValue.parse(jsonString);
                        contentModel.set(propName, value);
                    }
                }
            }
        } catch (Exception e) {
            throw new ProcessException(e);
        }
    }

}
