/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.workflow.mgt;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.workflow.mgt.bean.ServiceAssociationDTO;
import org.wso2.carbon.identity.workflow.mgt.bean.WSServiceBean;
import org.wso2.carbon.identity.workflow.mgt.bean.WorkflowEventBean;
import org.wso2.carbon.identity.workflow.mgt.bean.WorkflowEventParameterBean;
import org.wso2.carbon.identity.workflow.mgt.dao.WorkflowServicesDAO;
import org.wso2.carbon.identity.workflow.mgt.exception.InternalWorkflowException;
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowException;
import org.wso2.carbon.identity.workflow.mgt.internal.WorkflowMgtServiceComponent;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class WorkflowAdminService {

    private static Log log = LogFactory.getLog(WorkflowAdminService.class);

    WorkflowServicesDAO servicesDAO = new WorkflowServicesDAO();

    public void addWSService(WSServiceBean service) throws WorkflowException {
        if (service != null) {
            if (StringUtils.isBlank(service.getAlias())) {
                throw new WorkflowException("Service alias cannot be null or empty");
            }
            if (StringUtils.isBlank(service.getServiceEndpoint())) {
                throw new WorkflowException("Service endpoint cannot be null or empty");
            }
        }
        try {
            servicesDAO.addWorkflowService(service);
        } catch (InternalWorkflowException e) {
            log.error("Error while adding service.", e);
            throw new WorkflowException("Server error occurred when adding the service.");
        }
    }

    public void removeWSService(String alias, String event) throws WorkflowException {
        if (StringUtils.isBlank(alias)) {
            log.error("Null or empty string given as service alias to be removed.");
            throw new WorkflowException("Service alias cannot be null or empty");
        }
        try {
            servicesDAO.removeWorkflowAssociation(alias, event);
        } catch (InternalWorkflowException e) {
            log.error("Error while adding service.", e);
            throw new WorkflowException("Server error occurred when adding the service.");
        }
    }

    public void associateWSServiceToEvent(String serviceAlias, String eventType, String condition, int priority)
            throws WorkflowException {
        if (StringUtils.isBlank(serviceAlias)) {
            log.error("Null or empty string given as service alias to be associated to event.");
            throw new WorkflowException("Service alias cannot be null");
        }
        if (StringUtils.isBlank(eventType)) {
            log.error("Null or empty string given as 'event' to be associated with the service.");
            throw new WorkflowException("Event type cannot be null");
        }

        if (StringUtils.isBlank(condition)) {
            log.error("Null or empty string given as condition expression when associating " + serviceAlias +
                    " to event " +
                    eventType);
            throw new WorkflowException("Condition cannot be null");
        }

        //check for xpath syntax errors
        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        try {
            xpath.compile(condition);
            servicesDAO.associateServiceWithEvent(serviceAlias, eventType, condition, priority);
        } catch (XPathExpressionException e) {
            log.error("The condition:" + condition + " is not an valid xpath expression.", e);
            throw new WorkflowException("The condition:" + condition + " is not an valid xpath expression.");
        } catch (InternalWorkflowException e) {
            log.error("Error while associating service " + serviceAlias + " to event " + eventType, e);
            throw new WorkflowException(
                    "Server error occurred when associating the service " + serviceAlias + " to " + eventType);
        }
    }

    public WorkflowEventBean[] listWorkflowEvents() {
        Collection<WorkflowRequestHandler> workflowRequestHandlers =
                WorkflowMgtServiceComponent.getWorkflowRequestHandlers().values();
        WorkflowEventBean[] eventBeans = new WorkflowEventBean[workflowRequestHandlers.size()];
        int handlerIndex = 0;
        for (WorkflowRequestHandler workflowRequestHandler : workflowRequestHandlers) {
            WorkflowEventBean workflowEventBean = new WorkflowEventBean();
            workflowEventBean.setEventName(workflowRequestHandler.getEventId());
            List<WorkflowEventParameterBean> parameterBeanList = new ArrayList<>();
            for (Map.Entry<String, String> paramDefEntry : workflowRequestHandler.getParamDefinitions()
                    .entrySet()) {
                WorkflowEventParameterBean parameterBean = new WorkflowEventParameterBean();
                parameterBean.setParameterName(paramDefEntry.getKey());
                parameterBean.setParameterType(paramDefEntry.getValue());
                parameterBeanList.add(parameterBean);
            }
            workflowEventBean.setParameters(parameterBeanList.toArray(new
                    WorkflowEventParameterBean[parameterBeanList.size()]));
            eventBeans[handlerIndex] = workflowEventBean;
            handlerIndex++;
        }
        return eventBeans;
    }

    public ServiceAssociationDTO[] listWSServices() throws WorkflowException {
        List<ServiceAssociationDTO> dtoList;
        try {
            dtoList = servicesDAO.listServiceAssociations();
            return dtoList.toArray(new ServiceAssociationDTO[dtoList.size()]);
        } catch (InternalWorkflowException e) {
            log.error("Error while listing service associations", e);
            throw new WorkflowException(
                    "Server error occurred when listing services");
        }
    }
}
