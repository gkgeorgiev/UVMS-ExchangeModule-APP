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
package eu.europa.ec.fisheries.uvms.exchange.rest.service;

import java.util.Date;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.europa.ec.fisheries.schema.exchange.source.v1.GetLogListByQueryResponse;
import eu.europa.ec.fisheries.schema.exchange.v1.ExchangeListQuery;
import eu.europa.ec.fisheries.schema.exchange.v1.ExchangeLogStatusType;
import eu.europa.ec.fisheries.schema.exchange.v1.TypeRefType;
import eu.europa.ec.fisheries.uvms.common.DateUtils;
import eu.europa.ec.fisheries.uvms.exchange.rest.dto.PollQuery;
import eu.europa.ec.fisheries.uvms.exchange.rest.dto.ResponseDto;
import eu.europa.ec.fisheries.uvms.exchange.rest.dto.RestResponseCode;
import eu.europa.ec.fisheries.uvms.exchange.rest.dto.exchange.ListQueryResponse;
import eu.europa.ec.fisheries.uvms.exchange.rest.error.ErrorHandler;
import eu.europa.ec.fisheries.uvms.exchange.rest.mapper.ExchangeLogMapper;
import eu.europa.ec.fisheries.uvms.exchange.service.ExchangeLogService;
import eu.europa.ec.fisheries.uvms.rest.security.RequiresFeature;
import eu.europa.ec.fisheries.uvms.rest.security.UnionVMSFeature;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;

@Path("/exchange")
@Stateless
public class ExchangeLogRestResource {

    final static Logger LOG = LoggerFactory.getLogger(ExchangeLogRestResource.class);

    @EJB
    ExchangeLogService serviceLayer;
    @Context
    private HttpServletRequest request;

    /**
     *
     * @responseMessage 200 [Success]
     * @responseMessage 500 [Error]
     *
     * @summary Get a list of all exchangeLogs by search criterias
     *
     */
    @POST
    @Consumes(value = {MediaType.APPLICATION_JSON})
    @Produces(value = {MediaType.APPLICATION_JSON})
    @Path("/list")
    @RequiresFeature(UnionVMSFeature.viewExchange)
    public ResponseDto getLogListByCriteria(final ExchangeListQuery query) {
        LOG.info("Get list invoked in rest layer");
        try {
            //TODO query in swagger
            GetLogListByQueryResponse response = serviceLayer.getExchangeLogList(query);
            ListQueryResponse exchangeLogList = ExchangeLogMapper.mapToQueryResponse(response);
            //ExchangeMock.mockLogList(query);
            return new ResponseDto(exchangeLogList, RestResponseCode.OK);
        } catch (Exception ex) {
            LOG.error("[ Error when geting log list. ] {} ", ex.getMessage());
            return ErrorHandler.getFault(ex);
        }
    }

    @POST
    @Consumes(value = {MediaType.APPLICATION_JSON})
    @Produces(value = {MediaType.APPLICATION_JSON})
    @Path(value = "/poll")
    @RequiresFeature(UnionVMSFeature.viewExchange)
    public ResponseDto getPollStatus(PollQuery query) {
        try {
            LOG.debug("Get ExchangeLog status for Poll in rest layer");
            Date from = DateUtils.stringToDate(query.getStatusFromDate());
            Date to = DateUtils.stringToDate(query.getStatusToDate());
            List<ExchangeLogStatusType> response = serviceLayer.getExchangeStatusHistoryList(query.getStatus(), TypeRefType.POLL, from, to);
            return new ResponseDto(response, RestResponseCode.OK);
        } catch (Exception e) {
            LOG.error("[ Error when getting config search fields. ]", e.getMessage());
            return ErrorHandler.getFault(e);
        }
    }

    @GET
    @Consumes(value = {MediaType.APPLICATION_JSON})
    @Produces(value = {MediaType.APPLICATION_JSON})
    @Path(value = "/poll/{typeRefGuid}")
    @RequiresFeature(UnionVMSFeature.viewExchange)
    public ResponseDto getPollStatus(@PathParam("typeRefGuid") String typeRefGuid) {
        try {
            LOG.debug("Get ExchangeLog status for Poll by typeRefGuid");
            ExchangeLogStatusType response = serviceLayer.getExchangeStatusHistory(TypeRefType.POLL, typeRefGuid, request.getRemoteUser());
            return new ResponseDto(response, RestResponseCode.OK);
        } catch (Exception e) {
            LOG.error("[ Error when getting config search fields. ]", e.getMessage());
            return ErrorHandler.getFault(e);
        }
    }

    @GET
    @Produces(value = {MediaType.APPLICATION_JSON})
    @Path("/{guid}")
    @RequiresFeature(UnionVMSFeature.viewExchange)
    public ResponseDto getExchangeLogByGuid(@PathParam("guid") String guid) {
        try {
            return new ResponseDto(serviceLayer.getExchangeLogByGuid(guid), RestResponseCode.OK);
        } catch (Exception e) {
            LOG.error("[ Error when getting exchange log by GUID. ] {}", e.getMessage());
            return ErrorHandler.getFault(e);
        }
    }

}