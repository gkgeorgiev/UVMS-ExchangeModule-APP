/*
 ﻿Developed with the contribution of the European Commission - Directorate General for Maritime Affairs and Fisheries
 © European Union, 2015-2016.

 This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can
 redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 Free Software Foundation, either version 3 of the License, or any later version. The IFDM Suite is distributed in
 the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details. You should have received a
 copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.
 */
package eu.europa.ec.fisheries.uvms.exchange.service.bean;

import eu.europa.ec.fisheries.schema.exchange.common.v1.AcknowledgeType;
import eu.europa.ec.fisheries.schema.exchange.module.v1.*;
import eu.europa.ec.fisheries.schema.exchange.movement.v1.MovementBaseType;
import eu.europa.ec.fisheries.schema.exchange.movement.v1.MovementRefType;
import eu.europa.ec.fisheries.schema.exchange.movement.v1.MovementSourceType;
import eu.europa.ec.fisheries.schema.exchange.movement.v1.SetReportMovementType;
import eu.europa.ec.fisheries.schema.exchange.plugin.types.v1.PluginFault;
import eu.europa.ec.fisheries.schema.exchange.plugin.types.v1.PluginType;
import eu.europa.ec.fisheries.schema.exchange.plugin.v1.AcknowledgeResponse;
import eu.europa.ec.fisheries.schema.exchange.plugin.v1.ExchangePluginMethod;
import eu.europa.ec.fisheries.schema.exchange.service.v1.ServiceResponseType;
import eu.europa.ec.fisheries.schema.exchange.service.v1.StatusType;
import eu.europa.ec.fisheries.schema.exchange.v1.*;
import eu.europa.ec.fisheries.schema.movement.module.v1.ProcessedMovementAck;
import eu.europa.ec.fisheries.schema.rules.movement.v1.RawMovementType;
import eu.europa.ec.fisheries.uvms.exchange.message.constants.MessageQueue;
import eu.europa.ec.fisheries.uvms.exchange.message.event.*;
import eu.europa.ec.fisheries.uvms.exchange.message.event.carrier.ExchangeMessageEvent;
import eu.europa.ec.fisheries.uvms.exchange.message.event.carrier.PluginMessageEvent;
import eu.europa.ec.fisheries.uvms.exchange.message.event.registry.PluginErrorEvent;
import eu.europa.ec.fisheries.uvms.exchange.message.producer.MessageProducer;
import eu.europa.ec.fisheries.uvms.exchange.model.constant.FaultCode;
import eu.europa.ec.fisheries.uvms.exchange.model.exception.ExchangeException;
import eu.europa.ec.fisheries.uvms.exchange.model.exception.ExchangeModelMarshallException;
import eu.europa.ec.fisheries.uvms.exchange.model.mapper.ExchangeModuleResponseMapper;
import eu.europa.ec.fisheries.uvms.exchange.model.mapper.ExchangePluginResponseMapper;
import eu.europa.ec.fisheries.uvms.exchange.model.mapper.JAXBMarshaller;
import eu.europa.ec.fisheries.uvms.exchange.service.ExchangeEventIncomingService;
import eu.europa.ec.fisheries.uvms.exchange.service.ExchangeLogService;
import eu.europa.ec.fisheries.uvms.exchange.service.ExchangeRulesService;
import eu.europa.ec.fisheries.uvms.exchange.service.ExchangeService;
import eu.europa.ec.fisheries.uvms.exchange.service.event.ExchangePluginStatusEvent;
import eu.europa.ec.fisheries.uvms.exchange.service.event.PollEvent;
import eu.europa.ec.fisheries.uvms.exchange.service.exception.ExchangeLogException;
import eu.europa.ec.fisheries.uvms.exchange.service.exception.ExchangeServiceException;
import eu.europa.ec.fisheries.uvms.exchange.service.mapper.ExchangeLogMapper;
import eu.europa.ec.fisheries.uvms.exchange.service.mapper.MovementMapper;
import eu.europa.ec.fisheries.uvms.longpolling.notifications.NotificationMessage;
import eu.europa.ec.fisheries.uvms.movement.model.mapper.MovementModuleResponseMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.TextMessage;
import java.util.List;

@Stateless
public class ExchangeEventIncomingServiceBean implements ExchangeEventIncomingService {

    final static Logger LOG = LoggerFactory.getLogger(ExchangeEventIncomingServiceBean.class);

    @Inject
    @ErrorEvent
    Event<ExchangeMessageEvent> exchangeErrorEvent;

    @Inject
    @PluginErrorEvent
    Event<PluginMessageEvent> pluginErrorEvent;

    @EJB
    ExchangeLogService exchangeLog;

    @EJB
    MessageProducer producer;

    @EJB
    ExchangeService exchangeService;

    @EJB
    ExchangeRulesService rulesService;

    @Inject
    @ExchangePluginStatusEvent
    Event<NotificationMessage> pluginStatusEvent;

    @Inject
    @PollEvent
    Event<NotificationMessage> pollEvent;


    @Override
    public void getPluginListByTypes(@Observes @PluginConfigEvent ExchangeMessageEvent message) {
        LOG.info("Get plugin config LIST_SERVICE");
        try {
            TextMessage jmsMessage = message.getJmsMessage();
            GetServiceListRequest request = JAXBMarshaller.unmarshallTextMessage(jmsMessage, GetServiceListRequest.class);
            List<ServiceResponseType> serviceList = exchangeService.getServiceList(request.getType());
            producer.sendModuleResponseMessage(message.getJmsMessage(), ExchangeModuleResponseMapper.mapServiceListResponse(serviceList));
        } catch (ExchangeException e) {
            LOG.error("[ Error when getting plugin list from source ]");
            exchangeErrorEvent.fire(new ExchangeMessageEvent(message.getJmsMessage(), ExchangeModuleResponseMapper.createFaultMessage(
                    FaultCode.EXCHANGE_MESSAGE, "Excpetion when getting service list")));
        }
    }

    @Override
    public void processMovement(@Observes @SetMovementEvent ExchangeMessageEvent message) {
        LOG.info("Process movement");
        try {
            SetMovementReportRequest request = JAXBMarshaller.unmarshallTextMessage(message.getJmsMessage(), SetMovementReportRequest.class);
            String username;

            // A person has created a position
            if (MovementSourceType.MANUAL.equals(request.getRequest().getMovement().getSource())) {
                username = request.getUsername();

                // Send some response to Movement, if it originated from there (manual movement)
                ProcessedMovementAck response = MovementModuleResponseMapper.mapProcessedMovementAck(eu.europa.ec.fisheries.schema.movement.common.v1.AcknowledgeTypeType.OK, message.getJmsMessage().getJMSMessageID(), "Movement successfully processed");
                producer.sendModuleAckMessage(message.getJmsMessage().getJMSMessageID(), MessageQueue.MOVEMENT_RESPONSE, JAXBMarshaller.marshallJaxBObjectToString(response));
            } // A plugin has reported a position
            else {
                username = request.getRequest().getPluginType().name();
            }

            String pluginName = request.getRequest().getPluginName();
            PluginType pluginType = request.getRequest().getPluginType();

            ServiceResponseType service = exchangeService.getService(pluginName);
            LOG.debug("Process movement from {} of {} type", pluginName, pluginType);

            if (validate(request.getRequest(), service, message.getJmsMessage())) {
                MovementBaseType baseMovement = request.getRequest().getMovement();
                RawMovementType rawMovement = MovementMapper.getInstance().getMapper().map(baseMovement, RawMovementType.class);
                if (rawMovement.getAssetId() != null && rawMovement.getAssetId().getAssetIdList() != null) {
                    rawMovement.getAssetId().getAssetIdList().addAll(MovementMapper.mapAssetIdList(baseMovement.getAssetId().getAssetIdList()));
                }
                if (baseMovement.getMobileTerminalId() != null && baseMovement.getMobileTerminalId().getMobileTerminalIdList() != null) {
                    rawMovement.getMobileTerminal().getMobileTerminalIdList().addAll(MovementMapper.mapMobileTerminalIdList(baseMovement.getMobileTerminalId().getMobileTerminalIdList()));
                }

                rawMovement.setPluginType(pluginType.value());
                rawMovement.setPluginName(pluginName);
                rawMovement.setDateRecieved(request.getRequest().getTimestamp());
                // TODO: Temporary - probably better to change corr id to have the same though the entire flow; then we can use this to send response to original caller from anywhere needed
                rawMovement.setAckResponseMessageID(message.getJmsMessage().getJMSMessageID());

                try {
                    rulesService.sendMovementToRules(MovementMapper.mapPluginType(pluginType), rawMovement, username);
                } catch (ExchangeServiceException e) {
                    PluginFault fault = ExchangePluginResponseMapper.mapToPluginFaultResponse(FaultCode.EXCHANGE_PLUGIN_EVENT.getCode(), "Movement sent cannot be sent to Rules module [ " + e.getMessage() + " ]");
                    pluginErrorEvent.fire(new PluginMessageEvent(message.getJmsMessage(), service, fault));
                }
            } else {
                LOG.debug("Validation error. Event sent to plugin");
            }

        } catch (ExchangeServiceException e) {
            //TODO send back to plugin
        } catch (ExchangeModelMarshallException e) {
            //Cannot send back fault to unknown sender
            LOG.error("Couldn't map to SetMovementReportRequest when processing movement from plugin");
        } catch (JMSException e) {
            LOG.error("Failed to get response queue");
        }
    }

    // Asynch response handler for processed movements
    @Override
    public void handleProcessedMovement(@Observes @HandleProcessedMovementEvent ExchangeMessageEvent message) {
        LOG.debug("Received processed movement from Rules");
        try {
            ProcessedMovementResponse request = JAXBMarshaller.unmarshallTextMessage(message.getJmsMessage(), ProcessedMovementResponse.class);
            String username;
            MovementRefType movementRefType = request.getMovementRefType();
            SetReportMovementType orgRequest = request.getOrgRequest();

            if (PluginType.MANUAL.equals(orgRequest.getPluginType())) {
                username = request.getUsername();
            } else {
                username = orgRequest.getPluginName();
            }

            ExchangeLogType log = ExchangeLogMapper.getReceivedMovementExchangeLog(orgRequest, movementRefType.getMovementRefGuid(), movementRefType.getType().value(), username);
            ExchangeLogType createdLog = exchangeLog.log(log, username);

            LogRefType logTypeRef = createdLog.getTypeRef();
            if (logTypeRef != null && logTypeRef.getType() == TypeRefType.POLL) {
                String pollGuid = logTypeRef.getRefGuid();
                pollEvent.fire(new NotificationMessage("guid", pollGuid));
            }
        } catch (ExchangeLogException | ExchangeModelMarshallException e) {
            LOG.error(e.getMessage());
        }
    }

    private boolean validate(SetReportMovementType setReport, ServiceResponseType service, TextMessage origin) {
        if (setReport == null) {
            String faultMessage = "No setReport request";
            pluginErrorEvent.fire(new PluginMessageEvent(origin, service, ExchangePluginResponseMapper.mapToPluginFaultResponse(FaultCode.PLUGIN_VALIDATION.getCode(), faultMessage)));
            return false;
        } else if (setReport.getMovement() == null) {
            String faultMessage = "No movement in setReport request";
            pluginErrorEvent.fire(new PluginMessageEvent(origin, service, ExchangePluginResponseMapper.mapToPluginFaultResponse(FaultCode.PLUGIN_VALIDATION.getCode(), faultMessage)));
            return false;
        } else if (setReport.getPluginType() == null) {
            String faultMessage = "No pluginType in setReport request";
            pluginErrorEvent.fire(new PluginMessageEvent(origin, service, ExchangePluginResponseMapper.mapToPluginFaultResponse(FaultCode.PLUGIN_VALIDATION.getCode(), faultMessage)));
            return false;
        } else if (setReport.getPluginName() == null || setReport.getPluginName().isEmpty()) {
            String faultMessage = "No pluginName in setReport request";
            pluginErrorEvent.fire(new PluginMessageEvent(origin, service, ExchangePluginResponseMapper.mapToPluginFaultResponse(FaultCode.PLUGIN_VALIDATION.getCode(), faultMessage)));
            return false;
        } else if (setReport.getTimestamp() == null) {
            String faultMessage = "No timestamp in setReport request";
            pluginErrorEvent.fire(new PluginMessageEvent(origin, service, ExchangePluginResponseMapper.mapToPluginFaultResponse(FaultCode.PLUGIN_VALIDATION.getCode(), faultMessage)));
            return false;
        }
        return true;
    }

    @Override
    public void ping(@Observes @PingEvent ExchangeMessageEvent message) {
        try {
            PingResponse response = new PingResponse();
            response.setResponse("pong");
            producer.sendModuleResponseMessage(message.getJmsMessage(), JAXBMarshaller.marshallJaxBObjectToString(response));
        } catch (ExchangeModelMarshallException e) {
            LOG.error("[ Error when marshalling ping response ]");
        }
    }

    @Override
    public void processPluginPing(@Observes @PluginPingEvent ExchangeMessageEvent message) {
        try {
            eu.europa.ec.fisheries.schema.exchange.plugin.v1.PingResponse response = JAXBMarshaller.unmarshallTextMessage(message.getJmsMessage(), eu.europa.ec.fisheries.schema.exchange.plugin.v1.PingResponse.class);
            //TODO handle ping response from plugin, eg. no serviceClassName in response
            LOG.info("FIX ME handle ping response from plugin");
        } catch (ExchangeModelMarshallException e) {
            LOG.error("Couldn't process ping response from plugin " + e.getMessage());
        }
    }

    @Override
    public void processAcknowledge(@Observes @ExchangeLogEvent ExchangeMessageEvent message) {
        LOG.info("Process acknowledge");

        try {
            AcknowledgeResponse response = JAXBMarshaller.unmarshallTextMessage(message.getJmsMessage(), AcknowledgeResponse.class);
            AcknowledgeType acknowledge = response.getResponse();
            String serviceClassName = response.getServiceClassName();
            ExchangePluginMethod method = response.getMethod();
            switch (method) {
                case SET_COMMAND:
                    // Only Acknowledge for poll should have a poll status set
                    if (acknowledge.getPollStatus() != null && acknowledge.getPollStatus().getPollId() != null) {
                        handleSetPollStatusAcknowledge(method, serviceClassName, acknowledge);
                    } else {
                        handleUpdateExchangeLogAcknowledge(method, serviceClassName, acknowledge);
                    }
                    break;
                case SET_REPORT:
                    handleUpdateExchangeLogAcknowledge(method, serviceClassName, acknowledge);
                    break;
                case START:
                    handleUpdateServiceAcknowledge(serviceClassName, acknowledge, StatusType.STARTED);
                    pluginStatusEvent.fire(createNotificationMessage(serviceClassName, true));
                    break;
                case STOP:
                    handleUpdateServiceAcknowledge(serviceClassName, acknowledge, StatusType.STOPPED);
                    pluginStatusEvent.fire(createNotificationMessage(serviceClassName, false));
                    break;
                case SET_CONFIG:
                default:
                    handleAcknowledge(method, serviceClassName, acknowledge);
                    break;
            }
        } catch (ExchangeModelMarshallException e) {
            LOG.error("Process acknowledge couldn't be marshalled");
        } catch (ExchangeServiceException e) {
            //TODO Audit.log() couldn't process acknowledge in exchange service
            LOG.error("Couldn't process acknowledge in exchange service: " + e.getMessage());
        }
    }

    private void handleUpdateExchangeLogAcknowledge(ExchangePluginMethod method, String serviceClassName, AcknowledgeType ack) {
        LOG.debug(method + " was acknowledged in " + serviceClassName);

        ExchangeLogStatusTypeType logStatus = ExchangeLogStatusTypeType.FAILED;
        switch (ack.getType()) {
            case OK:
                //TODO if(poll probably transmitted)
                logStatus = ExchangeLogStatusTypeType.SUCCESSFUL;
                try {
                    exchangeLog.removeUnsentMessage(ack.getUnsentMessageGuid(), serviceClassName);
                } catch (ExchangeLogException ex) {
                    LOG.error(ex.getMessage());
                }
                break;
            case NOK:
                LOG.debug(method + " was NOK: " + ack.getMessage());
                break;
        }

        try {
            ExchangeLogType updatedLog = exchangeLog.updateStatus(ack.getMessageId(), logStatus, serviceClassName);

            // Long polling
            LogRefType typeRef = updatedLog.getTypeRef();
            if (typeRef != null && typeRef.getType() == TypeRefType.POLL) {
                String pollGuid = typeRef.getRefGuid();
                pollEvent.fire(new NotificationMessage("guid", pollGuid));
            }
        } catch (ExchangeLogException e) {
            LOG.error(e.getMessage());
        }
    }

    private void handleSetPollStatusAcknowledge(ExchangePluginMethod method, String serviceClassName, AcknowledgeType ack) {
        LOG.debug(method + " was acknowledged in " + serviceClassName);
        try {
            PollStatus updatedLog = exchangeLog.setPollStatus(ack.getMessageId(), ack.getPollStatus().getPollId(), ack.getPollStatus().getStatus(), serviceClassName);

            // Long polling
            pollEvent.fire(new NotificationMessage("guid", updatedLog.getPollGuid()));
        } catch (ExchangeLogException e) {
            LOG.error(e.getMessage());
        }
    }

    private void handleUpdateServiceAcknowledge(String serviceClassName, AcknowledgeType ack, StatusType status) throws ExchangeServiceException {
        switch (ack.getType()) {
            case OK:
                exchangeService.updateServiceStatus(serviceClassName, status, serviceClassName);
                break;
            case NOK:
                //TODO Audit.log()
                LOG.error("Couldn't start service " + serviceClassName);
                break;
        }
    }

    private void handleAcknowledge(ExchangePluginMethod method, String serviceClassName, AcknowledgeType ack) {
        LOG.debug(method + " was acknowledged in " + serviceClassName);
        switch (ack.getType()) {
            case OK:
                break;
            case NOK:
                //TODO Audit.log()
                LOG.error(serviceClassName + " didn't like it. " + ack.getMessage());
                break;
        }
    }

    private NotificationMessage createNotificationMessage(String serviceClassName, boolean started) {
        NotificationMessage msg = new NotificationMessage("serviceClassName", serviceClassName);
        msg.setProperty("started", started);
        return msg;
    }

}
