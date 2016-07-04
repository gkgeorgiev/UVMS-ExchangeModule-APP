/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.europa.ec.fisheries.uvms.exchange.model.mapper;

import javax.xml.datatype.XMLGregorianCalendar;

import eu.europa.ec.fisheries.schema.exchange.common.v1.CommandType;
import eu.europa.ec.fisheries.schema.exchange.common.v1.ReportType;
import eu.europa.ec.fisheries.schema.exchange.common.v1.ReportTypeType;
import eu.europa.ec.fisheries.schema.exchange.movement.v1.SendMovementToPluginType;
import eu.europa.ec.fisheries.schema.exchange.plugin.v1.ExchangePluginMethod;
import eu.europa.ec.fisheries.schema.exchange.plugin.v1.PingRequest;
import eu.europa.ec.fisheries.schema.exchange.plugin.v1.SetCommandRequest;
import eu.europa.ec.fisheries.schema.exchange.plugin.v1.SetConfigRequest;
import eu.europa.ec.fisheries.schema.exchange.plugin.v1.SetMdrPluginRequest;
import eu.europa.ec.fisheries.schema.exchange.plugin.v1.SetReportRequest;
import eu.europa.ec.fisheries.schema.exchange.plugin.v1.StartRequest;
import eu.europa.ec.fisheries.schema.exchange.plugin.v1.StopRequest;
import eu.europa.ec.fisheries.schema.exchange.service.v1.SettingListType;
import eu.europa.ec.fisheries.uvms.exchange.model.exception.ExchangeModelMarshallException;
import eu.europa.ec.fisheries.schema.exchange.plugin.v1.SetFLUXFAResponseRequest;

/**
 *
 * @author jojoha
 */
public class ExchangePluginRequestMapper {

    public static String createSetReportRequest(XMLGregorianCalendar dateReceived, SendMovementToPluginType movement, String unsentMessageGuid) throws ExchangeModelMarshallException {
        SetReportRequest request = new SetReportRequest();
        request.setMethod(ExchangePluginMethod.SET_REPORT);
        ReportType reportType = new ReportType();
        reportType.setRecipient(movement.getRecipient());
        reportType.getRecipientInfo().addAll(movement.getRecipientInfo());
        reportType.setMovement(movement.getMovement());
        reportType.setTimestamp(dateReceived);
        reportType.setType(ReportTypeType.MOVEMENT);
        reportType.setUnsentMessageGuid(unsentMessageGuid);
        request.setReport(reportType);
        return JAXBMarshaller.marshallJaxBObjectToString(request);
    }

    public static String createSetCommandRequest(CommandType commandType) throws ExchangeModelMarshallException {
        SetCommandRequest request = new SetCommandRequest();
        request.setMethod(ExchangePluginMethod.SET_COMMAND);
        request.setCommand(commandType);
        return JAXBMarshaller.marshallJaxBObjectToString(request);
    }

    public static String createSetConfigRequest(SettingListType settingList) throws ExchangeModelMarshallException {
        SetConfigRequest request = new SetConfigRequest();
        request.setMethod(ExchangePluginMethod.SET_CONFIG);
        request.setConfigurations(settingList);
        return JAXBMarshaller.marshallJaxBObjectToString(request);
    }

    public static String createPingRequest() throws ExchangeModelMarshallException {
        PingRequest request = new PingRequest();
        request.setMethod(ExchangePluginMethod.PING);
        return JAXBMarshaller.marshallJaxBObjectToString(request);
    }

    public static String createStartRequest() throws ExchangeModelMarshallException {
        StartRequest request = new StartRequest();
        request.setMethod(ExchangePluginMethod.START);
        return JAXBMarshaller.marshallJaxBObjectToString(request);
    }

    public static String createStopRequest() throws ExchangeModelMarshallException {
        StopRequest request = new StopRequest();
        request.setMethod(ExchangePluginMethod.STOP);
        return JAXBMarshaller.marshallJaxBObjectToString(request);
    }
    
	/**
	 * Maps the MDR Request that has to be sent to the MDR plugin to string. 
	 * 
	 * @param mdrBaseRequest
	 * @return
	 * @throws ExchangeModelMarshallException
	 */
	public static String mapMdrRequestToPluginBaseRequest(String mdrBaseRequest) throws ExchangeModelMarshallException{
		SetMdrPluginRequest pluginRequest = new SetMdrPluginRequest();
		pluginRequest.setMethod(ExchangePluginMethod.SET_MDR_REQUEST);
		pluginRequest.setRequest(mdrBaseRequest);
		return JAXBMarshaller.marshallJaxBObjectToString(pluginRequest);
	}


    public static String createSetFLUXFAResponseRequest(String  fluxFAResponse) throws ExchangeModelMarshallException {
        SetFLUXFAResponseRequest request = new SetFLUXFAResponseRequest();
        request.setMethod(ExchangePluginMethod.SET_FLUX_RESPONSE);
        request.setResponse(fluxFAResponse);
        return JAXBMarshaller.marshallJaxBObjectToString(request);
    }

}
