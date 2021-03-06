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

package danta.aem.contextprocessors.lists;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.google.common.collect.Sets;
import danta.aem.util.GeneralRequestObjects;
import danta.api.ExecutionContext;
import danta.api.TemplateContentModel;
import danta.api.exceptions.ProcessException;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import java.util.*;

import static danta.Constants.*;
import static danta.aem.Constants.SLING_HTTP_REQUEST;

/**
 * This Context Processor adds to the content model a list of page paths in 'list.pageRefs' and
 * add their page details in 'list.pages'.
 *
 * @author      Danta Team
 * @version     1.0.0
 * @since       2014-08-16
 */
@Component
@Service
public class AddPageDetailsContextProcessor
        extends AbstractPageDetailsContextProcessor {

    protected static final int PRIORITY = AddCuratedPageReferencesContextProcessor.PRIORITY - 20;

    @Override
    public Set<String> allOf() {
        return Sets.newHashSet(PAGE_DETAILS_CATEGORY);
    }

    @Override
    public Set<String> anyOf() {
        return Sets.newHashSet(CURATED_LIST_CATEGORY, TRAVERSED_LIST_CATEGORY);
    }

    @Override
    public int priority() {
        return PRIORITY;
    }

    @Override
    public void process(final ExecutionContext executionContext, TemplateContentModel contentModel)
            throws ProcessException {
        try {
            SlingHttpServletRequest request = (SlingHttpServletRequest) executionContext.get(SLING_HTTP_REQUEST);
            ResourceResolver resourceResolver = request.getResourceResolver();
            PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
            if (contentModel.has(LIST_PROPERTIES_KEY + DOT + PAGEREFS_CONTENT_KEY_NAME)) {
                Collection<Map<String,Object>> pathList = contentModel.getAs(LIST_PROPERTIES_KEY + DOT + PAGEREFS_CONTENT_KEY_NAME, Collection.class);
                List<Map<String, Object>> allPageDetailList = new ArrayList<>();
                String currentPage = GeneralRequestObjects.getCurrentPage(request).getPath();
                for (Map<String,Object> pathInfo: pathList) {
                    allPageDetailList.add(extractPageDetails(pathInfo, pageManager, request.getResource(), currentPage));
                }
                contentModel.set(PAGE_DETAILS_LIST_CONTEXT_PROPERTY_NAME, allPageDetailList);
            }
        } catch (Exception e) {
            throw new ProcessException(e);
        }
    }

    public Map<String, Object> extractPageDetails(Map<String, Object> pathInfo,PageManager pageManager, Resource componentResource, String currentPage) throws Exception{
        Map<String, Object> pageDetails = new HashMap<>();
        String path = pathInfo.get(PATH_DETAILS_LIST_PATH_PROPERTY_NAME).toString();
        Page page = pageManager.getPage(path);
        if (null != page){
            pageDetails = extractBasicPageDetails(page, componentResource, currentPage);

            Collection<Map<String, Object>> paths = (Collection<Map<String,Object>>)pathInfo.get(PATH_DETAILS_LIST_PATHS_PROPERTY_NAME);
            if(paths != null) {
                Collection<Map<String, Object>> pageChildrenDetails = new ArrayList<>();
                for (Map<String, Object> childPathInfo : paths) {
                    pageChildrenDetails.add(extractPageDetails(childPathInfo, pageManager, componentResource, currentPage));
                }
                pageDetails.put(PAGE_LIST_CONTEXT_PROPERTY_NAME,pageChildrenDetails);
            }
        }
        return pageDetails;
    }

}
