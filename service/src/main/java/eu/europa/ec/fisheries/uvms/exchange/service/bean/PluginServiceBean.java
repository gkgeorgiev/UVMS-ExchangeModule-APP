package eu.europa.ec.fisheries.uvms.exchange.service.bean;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.jms.TextMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.europa.ec.fisheries.schema.exchange.common.v1.AcknowledgeTypeType;
import eu.europa.ec.fisheries.schema.exchange.plugin.types.v1.PluginType;
import eu.europa.ec.fisheries.schema.exchange.registry.v1.RegisterServiceRequest;
import eu.europa.ec.fisheries.schema.exchange.registry.v1.UnregisterServiceRequest;
import eu.europa.ec.fisheries.schema.exchange.service.v1.ServiceResponseType;
import eu.europa.ec.fisheries.schema.exchange.service.v1.ServiceType;
import eu.europa.ec.fisheries.schema.exchange.service.v1.SettingListType;
import eu.europa.ec.fisheries.schema.exchange.service.v1.SettingType;
import eu.europa.ec.fisheries.uvms.exchange.message.event.carrier.PluginMessageEvent;
import eu.europa.ec.fisheries.uvms.exchange.message.event.registry.PluginErrorEvent;
import eu.europa.ec.fisheries.uvms.exchange.message.event.registry.RegisterServiceEvent;
import eu.europa.ec.fisheries.uvms.exchange.message.event.registry.UnRegisterServiceEvent;
import eu.europa.ec.fisheries.uvms.exchange.message.exception.ExchangeMessageException;
import eu.europa.ec.fisheries.uvms.exchange.message.producer.MessageProducer;
import eu.europa.ec.fisheries.uvms.exchange.model.constant.FaultCode;
import eu.europa.ec.fisheries.uvms.exchange.model.exception.ExchangeModelMarshallException;
import eu.europa.ec.fisheries.uvms.exchange.model.mapper.ExchangePluginResponseMapper;
import eu.europa.ec.fisheries.uvms.exchange.model.mapper.JAXBMarshaller;
import eu.europa.ec.fisheries.uvms.exchange.service.ExchangeService;
import eu.europa.ec.fisheries.uvms.exchange.service.PluginService;
import eu.europa.ec.fisheries.uvms.exchange.service.exception.ExchangeServiceException;

@Stateless
public class PluginServiceBean implements PluginService {
	final static Logger LOG = LoggerFactory.getLogger(PluginServiceBean.class);
	
    @Inject
    @PluginErrorEvent
    Event<PluginMessageEvent> errorEvent;
	
	@EJB
	ExchangeService exchangeService;
	
    @EJB
    MessageProducer producer;
	
	@Override
	public void registerService(@Observes @RegisterServiceEvent PluginMessageEvent event) {
		LOG.info("register service");
		TextMessage textMessage = event.getJmsMessage();
		String serviceName = null;
		try {
			RegisterServiceRequest register = JAXBMarshaller.unmarshallTextMessage(textMessage, RegisterServiceRequest.class);
	        serviceName = register.getResponseTopicMessageSelector();
	        boolean sendMessage = true;
	        
	        if(register.getService() != null) {
	        	PluginType pluginType = register.getService().getPluginType();
		        if(PluginType.EMAIL == pluginType || PluginType.FLUX == pluginType) {
		        	//Check if type already exists
		        	List<PluginType> type = new ArrayList<>();
		        	type.add(pluginType);
			        List<ServiceResponseType> services = exchangeService.getServiceList(type);
			        if(!services.isEmpty()) {
			        	
			        	//TODO log to exchange log
			        	//TODO better response message
			        	String response = ExchangePluginResponseMapper.mapToRegisterServiceResponse(AcknowledgeTypeType.NOK, services.get(0), null);
			        	producer.sendEventBusMessage(response, serviceName);
			        	sendMessage = false;
			        }
		        }
		        
		        if(sendMessage) {
		        	ServiceResponseType service = exchangeService.registerService(register.getService(), register.getCapabilityList(), register.getSettingList());
				
		        	//TODO set settings to parameter table, push to config module
				
		        	//TODO log to exchange log
		        
		        	//TODO receive settings
		        	SettingListType settings = null;
				
		        	String response = ExchangePluginResponseMapper.mapToRegisterServiceResponse(AcknowledgeTypeType.OK, service, settings);
		        	producer.sendEventBusMessage(response, serviceName);
		        }
	        }

		} catch (ExchangeModelMarshallException | ExchangeServiceException | ExchangeMessageException e) {
			LOG.error("Register service exception " + e.getMessage());
			errorEvent.fire(new PluginMessageEvent(textMessage, serviceName, ExchangePluginResponseMapper.mapToPluginFaultResponse(FaultCode.EXCHANGE_PLUGIN_EVENT.getCode(), "Exception when register service")));
		}
	}

	@Override
	public void unregisterService(@Observes @UnRegisterServiceEvent PluginMessageEvent event) {
		LOG.info("unregister service");
		TextMessage textMessage = event.getJmsMessage();
		String serviceName = null;
		try {
			UnregisterServiceRequest unregister = JAXBMarshaller.unmarshallTextMessage(textMessage, UnregisterServiceRequest.class);
			serviceName = unregister.getResponseTopicMessageSelector();
			
			ServiceType service = exchangeService.unregisterService(unregister.getService());
			
	        //TODO log to exchange log
	        
		} catch (ExchangeModelMarshallException | ExchangeServiceException e) {
			LOG.error("Unregister service exception " + e.getMessage());
			errorEvent.fire(new PluginMessageEvent(textMessage, serviceName, ExchangePluginResponseMapper.mapToPluginFaultResponse(FaultCode.EXCHANGE_PLUGIN_EVENT.getCode(), "Exception when unregister service")));
		}
	}
}
