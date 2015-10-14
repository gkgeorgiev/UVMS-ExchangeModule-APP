package eu.europa.ec.fisheries.uvms.exchange.service.mapper;

import java.util.ArrayList;
import java.util.List;

import org.dozer.DozerBeanMapper;

import eu.europa.ec.fisheries.schema.rules.asset.v1.AssetIdType;
import eu.europa.ec.fisheries.schema.rules.mobileterminal.v1.IdType;

public class MovementMapper {

	private static final DozerBeanMapper mapper = new DozerBeanMapper();

	private MovementMapper() {
	}

	public static DozerBeanMapper getMapper() {
		return mapper;
	}
	
	public static List<eu.europa.ec.fisheries.schema.rules.asset.v1.AssetIdList> mapAssetIdList(List<eu.europa.ec.fisheries.schema.exchange.movement.asset.v1.AssetIdList> inList) {
		List<eu.europa.ec.fisheries.schema.rules.asset.v1.AssetIdList> outList = new ArrayList<>();
		for(eu.europa.ec.fisheries.schema.exchange.movement.asset.v1.AssetIdList inAssetId : inList) {
			eu.europa.ec.fisheries.schema.rules.asset.v1.AssetIdList outAssetId = new eu.europa.ec.fisheries.schema.rules.asset.v1.AssetIdList();
			AssetIdType idType = null;
			switch(inAssetId.getIdType()) {
			case CFR:
				idType = AssetIdType.CFR;
				break;
			case ID:
				idType = AssetIdType.ID;
				break;
			case IMO:
				idType = AssetIdType.IMO;
				break;
			case IRCS:
				idType = AssetIdType.IRCS;
				break;
			case MMSI:
				idType = AssetIdType.MMSI;
				break;
			case GUID:
				idType = AssetIdType.GUID;
				break;
			}
			outAssetId.setIdType(idType);
			outAssetId.setValue(inAssetId.getValue());
			outList.add(outAssetId);
		}
		return outList;
	}
	
	public static List<eu.europa.ec.fisheries.schema.rules.mobileterminal.v1.IdList> mapMobileTerminalIdList(List<eu.europa.ec.fisheries.schema.exchange.movement.mobileterminal.v1.IdList> inList) {
		List<eu.europa.ec.fisheries.schema.rules.mobileterminal.v1.IdList> outList = new ArrayList<>();
		for(eu.europa.ec.fisheries.schema.exchange.movement.mobileterminal.v1.IdList inId : inList) {
			eu.europa.ec.fisheries.schema.rules.mobileterminal.v1.IdList outId = new eu.europa.ec.fisheries.schema.rules.mobileterminal.v1.IdList();
			IdType idType = null;
			switch(inId.getType()) {
			case DNID:
				idType = IdType.DNID;
				break;
			case LES:
				idType = IdType.LES;
				break;
			case MEMEBER_NUMBER:
				idType = IdType.MEMBER_NUMBER;
				break;
			case SATELLITE_NUMBER:
				idType = IdType.SATELLITE_NUMBER;
				break;
			}
			outId.setType(idType);
			outId.setValue(inId.getValue());
			outList.add(outId);
		}
		return outList;
	}
}