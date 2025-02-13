package com.socotra.deployment.customer.property;

import com.socotra.deployment.customer.*;
import com.socotra.coremodel.*;
import com.socotra.deployment.*;
import com.socotra.coremodel.RatingItem;
import com.socotra.coremodel.RatingSet;
import com.socotra.deployment.customer.PropertyQuote;
import com.socotra.deployment.customer.ChargeType;
import com.socotra.deployment.customer.RatePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class PropertyRatingPlugin implements RatePlugin {
        private static final Logger log = LoggerFactory.getLogger(PropertyRatingPlugin.class);

        @Override
        public RatingSet rate(PropertyQuoteRequest propertyQuoteRequest) {

                PropertyQuote quote = propertyQuoteRequest.quote();

                log.info("property quote", quote);

                List<RatingItem> ratingItems = new ArrayList<>();

               ResourceSelector tableRecordFetcher = ResourceSelectorFactory.getInstance().getSelector(quote);

               String state = "";
               int zipCode = 0;
               BigDecimal buildingRate = new BigDecimal("0.0");
               BigDecimal locationRate = new BigDecimal("0.0");

               BigDecimal testAmount = new BigDecimal("1.2");

               BigDecimal subtotalBuildingsRateProduct1 = new BigDecimal("0.0");
               BigDecimal subtotalBuildingsRateProduct2 = new BigDecimal("0.0");

               BigDecimal subtotalContentsRateProduct1 = new BigDecimal("0.0");
               BigDecimal subtotalContentsRateProduct2 = new BigDecimal("0.0");

               BigDecimal subtotalRateBuildings = new BigDecimal("0.0");
               BigDecimal subtotalRateContents = new BigDecimal("0.0");

               // Loop through all buildings
               for (PropertyLocation loc : quote.propertyLocations()) {
                zipCode = loc.data().packageProductBasicInfo().locationAddress().zipCode();
                log.info("zip-code: {}", Integer.toString(zipCode));

                state = loc.data().packageProductBasicInfo().locationAddress().state();
                log.info("state: {}", state);

                // Rating Location
                log.info("Rating the location: {}", loc.data());
                // TODO: clarify behavior around occupancy type for launch
                String occupancyType = loc.data().packageOccupancyInfo().tenantsOccupants().get(0);
                String classCode = tableRecordFetcher.getTable(PropertyOccupancyClassCodes.class).getRecord(PropertyOccupancyClassCodes.makeKey(occupancyType)).orElseThrow().classCode();
                if (classCode == null || classCode.length() == 0) {
                    classCode = loc.data().classCode();
                }
                log.info("location class-code: {}", classCode);

                for (PropertyBuilding building : loc.propertyBuildings()) {
                               BigDecimal contentValue = BigDecimal.valueOf(building.data().buildingCoverageTerms().contentValue());
                               log.info("content values of building: {}", contentValue);

                               String coinsurancePercent = loc.data().packageLocationCoverageTerms().coinsurancePercent();
                               log.info("Rating the building coinsurance: {}", coinsurancePercent);

                               BigDecimal buildingValue = BigDecimal.valueOf(building.data().buildingCoverageTerms().buildingValue());
                               log.info("Exposure building value: {}", buildingValue);

                               BigDecimal oneHundredth = new BigDecimal("100.0");
                               String windHailDeductibleString = building.data().buildingCoverageTerms().windHailDeductible();
                                BigDecimal windHailDeductible = new BigDecimal("1.00");
                                if (!windHailDeductibleString.equalsIgnoreCase("Exclude Wind/Hail Coverage")) {
                                    windHailDeductible = new BigDecimal(windHailDeductibleString.substring(0,
                                            windHailDeductibleString.length() - 1)).divide(oneHundredth);
                                }
                                log.info("Exposure building wind-hail: {}", windHailDeductible);

                               BigDecimal calculatedLOI = buildingValue.multiply(windHailDeductible);
                               log.info("calculatedLOI for building: ", calculatedLOI);

                                String constructionType = building.data().propertyConstructionUpdates()
                                        .constructionType();
                                log.info("building construction: {}", constructionType);

                               BigDecimal classBRGroup1 = tableRecordFetcher.getTable(BaseLCv2.class)
                                               .getRecord(BaseLCv2.makeKey(classCode, "Building", constructionType))
                                               .orElseThrow().buildingGroup1();

                               BigDecimal classBRGroup2 = tableRecordFetcher.getTable(BaseLCv2.class)
                                               .getRecord(BaseLCv2.makeKey(classCode, "Building",
                                                               constructionType))
                                               .orElseThrow().buildingGroup2();

                               BigDecimal classBaseRateGroups = (classBRGroup1)
                                               .add(classBRGroup2);
                               log.info("class base rate groups: {}", classBaseRateGroups);

                               BigDecimal classBRContent1 = tableRecordFetcher.getTable(BaseLCv2.class)
                                               .getRecord(BaseLCv2.makeKey(classCode, "Contents", constructionType))
                                               .orElseThrow().contentsGroup1();

                               BigDecimal classBRContent2 = tableRecordFetcher.getTable(BaseLCv2.class)
                                               .getRecord(BaseLCv2.makeKey(classCode, "Contents",
                                                               constructionType))
                                               .orElseThrow().contentsGroup2();

                               BigDecimal classBaseRateContents = (classBRContent1)
                                               .add(classBRContent2);
                               log.info("class base rate contents: {}", classBaseRateContents);

                               BigDecimal lcmGroup1 = tableRecordFetcher.getTable(LCM_Property.class)
                                               .getRecord(LCM_Property.makeKey(state)).orElseThrow().buildingGroup1();
                               BigDecimal lcmGroup2 = tableRecordFetcher.getTable(LCM_Property.class)
                                               .getRecord(LCM_Property.makeKey(state)).orElseThrow().buildingGroup2();

                               BigDecimal lcmBuildings = (((lcmGroup1)
                                               .multiply(classBRGroup1)).add((lcmGroup2)
                                                               .multiply(classBRGroup2)))
                                               .divide(classBaseRateGroups, 2, RoundingMode.HALF_EVEN);

                               log.info("lcm buildings: {}", lcmBuildings);

                               BigDecimal lcmContents1 = tableRecordFetcher.getTable(LCM_Property.class)
                                               .getRecord(LCM_Property.makeKey(state)).orElseThrow().contentsGroup1();

                               BigDecimal lcmContents2 = tableRecordFetcher.getTable(LCM_Property.class)
                                               .getRecord(LCM_Property.makeKey(state)).orElseThrow()
                                               .contentsGroup2();

                               BigDecimal lcmContents = (((lcmContents1)
                                               .multiply(classBRContent1)).add((lcmContents2)
                                                               .multiply(classBRContent2)))
                                               .divide(classBaseRateContents, 2, RoundingMode.HALF_EVEN);
                               log.info("lcm contents: {}", lcmContents);

                               String causeOfLossForm = loc.data().packageLocationCoverageTerms().causeOfLossForm();
                               log.info("building cause of loss form: {}", causeOfLossForm);

                               BigDecimal formBuilding1 = tableRecordFetcher.getTable(FormFactor.class)
                                               .getRecord(FormFactor.makeKey(causeOfLossForm)).orElseThrow()
                                               .building1();

                               BigDecimal formBuilding2 = tableRecordFetcher.getTable(FormFactor.class)
                                               .getRecord(FormFactor.makeKey(causeOfLossForm)).orElseThrow()
                                               .building2();

                               BigDecimal formBuildings = (((classBRGroup1)
                                               .multiply(formBuilding1))
                                               .add((classBRGroup2)
                                                               .multiply(formBuilding1)))
                                               .divide(classBaseRateGroups, 2, RoundingMode.HALF_EVEN);
                               log.info("building cause of loss form: {}", formBuildings);

                               BigDecimal formContent1 = tableRecordFetcher.getTable(FormFactor.class)
                                               .getRecord(FormFactor.makeKey(causeOfLossForm)).orElseThrow()
                                               .contents1();

                               BigDecimal formContent2 = tableRecordFetcher.getTable(FormFactor.class)
                                               .getRecord(FormFactor.makeKey(causeOfLossForm))
                                               .orElseThrow().contents2();

                               BigDecimal formContents = (((classBRContent1).multiply(formContent1))
                                               .add((classBRContent2)
                                                               .multiply(formContent2)))
                                               .divide(classBaseRateContents, 2, RoundingMode.HALF_EVEN);
                               log.info("contents cause of loss form: {}", formContents);

                               BigDecimal coinsuranceBuilding1 = tableRecordFetcher.getTable(CoinsuranceFactor.class)
                                               .getRecord(CoinsuranceFactor.makeKey(coinsurancePercent)).orElseThrow()
                                               .building1();
                               BigDecimal coinsuranceBuilding2 = tableRecordFetcher.getTable(CoinsuranceFactor.class)
                                               .getRecord(CoinsuranceFactor.makeKey(coinsurancePercent)).orElseThrow()
                                               .building2();

                               BigDecimal coinsuranceBuildings = (((classBRGroup1).multiply(coinsuranceBuilding1))
                                               .add((classBRGroup2)
                                                               .multiply(coinsuranceBuilding2)))
                                               .divide(classBaseRateGroups, 2, RoundingMode.HALF_EVEN);
                               log.info("coinsurance buildings: {}", coinsuranceBuildings);

                               BigDecimal coinsuranceContent1 = tableRecordFetcher.getTable(CoinsuranceFactor.class)
                                               .getRecord(CoinsuranceFactor.makeKey(coinsurancePercent)).orElseThrow()
                                               .contents1();

                               BigDecimal coinsuranceContent2 = tableRecordFetcher.getTable(CoinsuranceFactor.class)
                                               .getRecord(CoinsuranceFactor
                                                               .makeKey(coinsurancePercent))
                                               .orElseThrow().contents2();

                               BigDecimal coinsuranceContents = (((classBRContent1).multiply(coinsuranceContent1))
                                               .add((classBRContent2)
                                                               .multiply(coinsuranceContent2)))
                                               .divide(classBaseRateContents, 2, RoundingMode.HALF_EVEN);
                               log.info("coinsurance contents: {}", coinsuranceContents);

                               String coverageAmountHolder = "50,001";
                               // TODO: change the below.
                               String deductible = building.data().buildingCoverageTerms().aopDeductible();

                               BigDecimal deductibleBuilding1 = tableRecordFetcher.getTable(DeductibleFactor.class)
                                               .getRecord(DeductibleFactor.makeKey(coverageAmountHolder, deductible))
                                               .orElseThrow().buildingGroup1();

                               BigDecimal deductibleBuilding2 = tableRecordFetcher.getTable(DeductibleFactor.class)
                                               .getRecord(DeductibleFactor.makeKey(
                                                               coverageAmountHolder, deductible))
                                               .orElseThrow().buildingGroup2();

                               BigDecimal deductibleBuildings = (((classBRGroup1).multiply(deductibleBuilding1))
                                               .add((classBRGroup2)
                                                               .multiply(deductibleBuilding2)))
                                               .divide(classBaseRateGroups, 2, RoundingMode.HALF_EVEN);
                               log.info("deductible buildings: {}", deductibleBuildings);

                               BigDecimal deductibleContents1 = tableRecordFetcher.getTable(DeductibleFactor.class)
                                               .getRecord(DeductibleFactor.makeKey(coverageAmountHolder, deductible))
                                               .orElseThrow().contentsGroup1();

                               BigDecimal deductibleContents2 = tableRecordFetcher.getTable(DeductibleFactor.class)
                                               .getRecord(DeductibleFactor.makeKey(
                                                               coverageAmountHolder, deductible))
                                               .orElseThrow().contentsGroup2();

                               BigDecimal deductibleContents = (((classBRContent1).multiply(deductibleContents1))
                                               .add((classBRContent2)
                                                               .multiply(deductibleContents2)))
                                               .divide(classBaseRateContents, 2, RoundingMode.HALF_EVEN);
                               log.info("deductible contents: {}", deductibleContents);

                               BigDecimal whDeductibleBuilding1 = tableRecordFetcher
                                               .getTable(WindHailDeductible.class)
                                               .getRecord(WindHailDeductible.makeKey(windHailDeductibleString,
                                                               coverageAmountHolder))
                                               .orElseThrow().building1();

                               BigDecimal whDeductibleBuilding2 = tableRecordFetcher.getTable(WindHailDeductible.class)
                                               .getRecord(WindHailDeductible
                                                               .makeKey(windHailDeductibleString,
                                                                               coverageAmountHolder))
                                               .orElseThrow().building2();

                               BigDecimal whDeductibleBuildings = (((classBRGroup1).multiply(whDeductibleBuilding1))
                                               .add((classBRGroup2)
                                                               .multiply(whDeductibleBuilding2)))
                                               .divide(classBaseRateGroups, 2, RoundingMode.HALF_EVEN);
                               log.info("wh-deductible contents: {}", whDeductibleBuildings);

                               BigDecimal whDeductibleContents1 = tableRecordFetcher
                                               .getTable(WindHailDeductible.class)
                                               .getRecord(WindHailDeductible.makeKey(windHailDeductibleString,
                                                               coverageAmountHolder))
                                               .orElseThrow().contents1();

                               BigDecimal whDeductibleContents2 = tableRecordFetcher.getTable(WindHailDeductible.class)
                                               .getRecord(WindHailDeductible
                                                               .makeKey(windHailDeductibleString,
                                                                               coverageAmountHolder))
                                               .orElseThrow().contents2();

                               BigDecimal whDeductibleContents = (((classBRContent1).multiply(whDeductibleContents1))
                                               .add((classBRContent2)
                                                               .multiply(whDeductibleContents2)))
                                               .divide(classBaseRateContents, 2, RoundingMode.HALF_EVEN);
                               log.info("deductible contents: {}", whDeductibleContents);
                               // TODO: check
//                                String territoryNamegroup1 = "Austin (Central)";
                               String territoryNamegroup1 = tableRecordFetcher.getTable(TerritoryProperty.class)
                                               .getRecord(TerritoryProperty.makeKey(zipCode)).orElseThrow()
                                               .group1();
                                log.info("territory group 1 name: {}", territoryNamegroup1);
//                                String territoryNamegroup2 = "Austin (Central)";
                               String territoryNamegroup2 = tableRecordFetcher.getTable(TerritoryProperty.class)
                                               .getRecord(TerritoryProperty.makeKey(zipCode)).orElseThrow()
                                               .group2();
                                log.info("territory group 2 name: {}", territoryNamegroup2);

                               BigDecimal territory1 = tableRecordFetcher.getTable(Gp1Territory.class)
                                               .getRecord(Gp1Territory.makeKey(state, territoryNamegroup1))
                                               .orElseThrow()
                                               .structureFactor();
                               log.info("territory group 1 factor: {}", territory1);

                               BigDecimal territory2 = tableRecordFetcher.getTable(Gp2Territory.class)
                                               .getRecord(Gp2Territory.makeKey(state, territoryNamegroup2))
                                               .orElseThrow()
                                               .structureFactor();
                               log.info("territory group 2 factor: {}", territory2);

                               BigDecimal territoryBuildingRate = (((classBRGroup1).multiply(territory1))
                                               .add((classBRGroup2).multiply(territory2)))
                                               .divide(classBaseRateGroups, 2, RoundingMode.HALF_EVEN);
                               log.info("territory building rate: {}", territoryBuildingRate);

                               BigDecimal territoryContentRate = (((classBRContent1).multiply(territory1))
                                               .add((classBRContent2).multiply(territory2)))
                                               .divide(classBaseRateContents, 2, RoundingMode.HALF_EVEN);
                               log.info("territory content rate: {}", territoryContentRate);

                               // territory name may need to be adjusted for buildings and contents
                               BigDecimal windHailExcFactor = tableRecordFetcher.getTable(WindHailExc.class)
                                               .getRecord(WindHailExc.makeKey(state, territoryNamegroup1))
                                               .orElseThrow()
                                               .factor();
                               log.info("wind hail exc factor {}", windHailExcFactor);

                               BigDecimal windHailRateBuildings = (((classBRGroup1).multiply(windHailExcFactor))
                                               .add((classBRGroup2).multiply(windHailExcFactor)))
                                               .divide(classBaseRateGroups, 2, RoundingMode.HALF_EVEN);
                               log.info("wind hail exc buildings: {}", windHailRateBuildings);

                               BigDecimal windHailGroups = (((classBRContent1).multiply(windHailExcFactor))
                                               .add((classBRContent2).multiply(windHailExcFactor)))
                                               .divide(classBaseRateContents, 2, RoundingMode.HALF_EVEN);
                               log.info("wind hail exc groups: {}", windHailGroups);

                               // TODO: occupancy count and hazard calculations need to be confirmed
                               // TODO confirm palcement here
                               String limit = "50,000";
                               String constructionClass = "CC_1_3";
                               BigDecimal loiGroup1 = tableRecordFetcher.getTable(Limit_Ins_Table_Property.class)
                                               .getRecord(Limit_Ins_Table_Property.makeKey(limit, causeOfLossForm,
                                                               "Building", constructionClass))
                                               .orElseThrow()
                                               .buildingGroup1();
                               log.info("loi group1: {}", loiGroup1);

                               BigDecimal loiGroup2 = tableRecordFetcher.getTable(Limit_Ins_Table_Property.class)
                                               .getRecord(Limit_Ins_Table_Property.makeKey(limit, causeOfLossForm,
                                                       "Building", constructionClass))
                                               .orElseThrow()
                                               .buildingGroup2();
                               log.info("loi group 2: {}", loiGroup2);

                               BigDecimal loiBuildings = (((classBRGroup1).multiply(loiGroup1))
                                               .add((classBRGroup2)
                                                               .multiply(loiGroup2)))
                                               .divide(classBaseRateGroups, 2, RoundingMode.HALF_EVEN);
                               log.info("loi buildings: {}", loiBuildings);

                               BigDecimal loiContents1 = tableRecordFetcher.getTable(Limit_Ins_Table_Property.class)
                                               .getRecord(Limit_Ins_Table_Property.makeKey(limit, causeOfLossForm,
                                                              "Contents", constructionClass))
                                               .orElseThrow()
                                               .contentsGroup1();
                               log.info("loi group1: {}", loiContents1);

                               BigDecimal loiContents2 = tableRecordFetcher.getTable(Limit_Ins_Table_Property.class)
                                               .getRecord(Limit_Ins_Table_Property.makeKey(limit, causeOfLossForm,
                                                       "Contents", constructionClass))
                                               .orElseThrow()
                                               .contentsGroup2();
                               log.info("loi contents 2: {}", loiContents2);

                               BigDecimal loiContents = (((classBRContent1).multiply(loiContents1))
                                               .add((classBRContent2)
                                                               .multiply(loiContents2)))
                                               .divide(classBaseRateContents, 2, RoundingMode.HALF_EVEN);
                               log.info("loi contents: {}", loiContents);

                               String ppcFireClass = building.data().propertyConstructionUpdates().ppcFireClass();
                               log.info("ppc fire class: {}", ppcFireClass);

                               // TODO: needs to be checked with an insure since the table contains multiple
                               // types for construction
                               String foundationType = "Frame,JoistedMasonry,Non-Combustible";
                               log.info("foundation type: {}", foundationType);

                               BigDecimal protectionBulding1 = tableRecordFetcher.getTable(ProtectionClass.class)
                                               .getRecord(ProtectionClass.makeKey(ppcFireClass, foundationType))
                                               .orElseThrow()
                                               .buildingGroup1();

                               BigDecimal protectionBulding2 = tableRecordFetcher.getTable(ProtectionClass.class)
                                               .getRecord(ProtectionClass.makeKey(ppcFireClass, foundationType))
                                               .orElseThrow()
                                               .buildingGroup2();

                               BigDecimal ppcBuildings = (((classBRGroup1).multiply(protectionBulding1))
                                               .add((classBRGroup2)
                                                               .multiply(protectionBulding2)))
                                               .divide(classBaseRateGroups, 2, RoundingMode.HALF_EVEN);
                               log.info("ppc buildings: {}", ppcBuildings);

                               BigDecimal protectionGroups1 = tableRecordFetcher.getTable(ProtectionClass.class)
                                               .getRecord(ProtectionClass.makeKey(ppcFireClass, foundationType))
                                               .orElseThrow()
                                               .contentsGroup1();

                               BigDecimal protectionGroups2 = tableRecordFetcher.getTable(ProtectionClass.class)
                                               .getRecord(ProtectionClass.makeKey(ppcFireClass, foundationType))
                                               .orElseThrow()
                                               .contentsGroup2();

                               BigDecimal ppcContents = (((classBRContent1).multiply(protectionGroups1))
                                               .add((classBRContent2)
                                                               .multiply(protectionGroups2)))
                                               .divide(classBaseRateContents, 2, RoundingMode.HALF_EVEN);
                               log.info("ppc contents: {}", ppcContents);

                               String buildingCodeGrade = building.data().propertyBuildingDetails().buildingCodeGrade();
                               log.info("building code: {}", buildingCodeGrade);

                               String territoyBuildingCodeGrade = "All";
                               String concat = (state)
                                               .concat(territoyBuildingCodeGrade);

                               BigDecimal buildingCodeGradeBulding1 = tableRecordFetcher
                                               .getTable(BuildingCode_Property.class)
                                               .getRecord(BuildingCode_Property.makeKey(state,
                                                               territoyBuildingCodeGrade, concat, buildingCodeGrade))
                                               .orElseThrow()
                                               .buildingGroup1();

                               BigDecimal buildingCodeGradeBulding2 = tableRecordFetcher
                                               .getTable(BuildingCode_Property.class)
                                               .getRecord(BuildingCode_Property.makeKey(state,
                                                               territoyBuildingCodeGrade, concat, buildingCodeGrade))
                                               .orElseThrow()
                                               .buildingGroup2();

                               BigDecimal buildingCodeGradeBuildings = (((classBRGroup1)
                                               .multiply(buildingCodeGradeBulding1))
                                               .add((classBRGroup2)
                                                               .multiply(buildingCodeGradeBulding2)))
                                               .divide(classBaseRateGroups, 2, RoundingMode.HALF_EVEN);
                               log.info("building code grade buildings: {}", buildingCodeGradeBuildings);

                               BigDecimal buildingCodeGradeGroups1 = tableRecordFetcher
                                               .getTable(BuildingCode_Property.class)
                                               .getRecord(BuildingCode_Property.makeKey(state,
                                                               territoyBuildingCodeGrade, concat, buildingCodeGrade))
                                               .orElseThrow()
                                               .contentsGroup1();

                               BigDecimal buildingCodeGradeGroups2 = tableRecordFetcher
                                               .getTable(BuildingCode_Property.class)
                                               .getRecord(BuildingCode_Property.makeKey(state,
                                                               territoyBuildingCodeGrade, concat, buildingCodeGrade))
                                               .orElseThrow()
                                               .contentsGroup2();

                               BigDecimal buildingCodeGradeContents = (((classBRContent1)
                                               .multiply(buildingCodeGradeGroups1))
                                               .add((classBRContent2)
                                                               .multiply(buildingCodeGradeGroups2)))
                                               .divide(classBaseRateContents, 2, RoundingMode.HALF_EVEN);
                               log.info("building grade contents: {}", buildingCodeGradeContents);

                               String buildingAge = building.data().buildingAge();
                               log.info("building's age: {}", buildingAge);
                                // TODO fix these values
                               BigDecimal buildingAgeBulding1 = tableRecordFetcher.getTable(Build_Age_Fctr.class)
                                               .getRecord(Build_Age_Fctr.makeKey(buildingAge, buildingAge,
                                                               buildingAge))
                                               .orElseThrow()
                                               .buildingGroup1();

                               BigDecimal buildingAgeBulding2 = tableRecordFetcher.getTable(Build_Age_Fctr.class)
                                               .getRecord(Build_Age_Fctr.makeKey(buildingAge, buildingAge,
                                                               buildingAge))
                                               .orElseThrow()
                                               .buildingGroup2();

                               BigDecimal buildingAgeBuildings = (((classBRGroup1).multiply(buildingAgeBulding1))
                                               .add((classBRGroup2)
                                                               .multiply(buildingAgeBulding2)))
                                               .divide(classBaseRateGroups, 2, RoundingMode.HALF_EVEN);
                               log.info("building age buildings: {}", buildingAgeBuildings);

                               BigDecimal buildingAgeGroups1 = tableRecordFetcher.getTable(Build_Age_Fctr.class)
                                               .getRecord(Build_Age_Fctr.makeKey(buildingAge, buildingAge,
                                                               buildingAge))
                                               .orElseThrow()
                                               .contentsGroup1();

                               BigDecimal buildingAgeGroups2 = tableRecordFetcher.getTable(Build_Age_Fctr.class)
                                               .getRecord(Build_Age_Fctr.makeKey(buildingAge, buildingAge,
                                                               buildingAge))
                                               .orElseThrow()
                                               .contentsGroup2();

                               BigDecimal buildingAgeContents = (((classBRContent1).multiply(buildingAgeGroups1))
                                               .add((classBRContent2)
                                                               .multiply(buildingAgeGroups2)))
                                               .divide(classBaseRateContents, 2, RoundingMode.HALF_EVEN);
                               log.info("building age contents: {}", buildingAgeContents);

                               String roofType = building.data().propertyConstructionUpdates().roofCovering();
                               log.info("roof type: {}", roofType);

                               BigDecimal roofTypeBulding1 = tableRecordFetcher.getTable(Roof_Type_Fctr.class)
                                               .getRecord(Roof_Type_Fctr.makeKey(roofType))
                                               .orElseThrow()
                                               .buildingGroup1();

                               BigDecimal roofTypeBulding2 = tableRecordFetcher.getTable(Roof_Type_Fctr.class)
                                               .getRecord(Roof_Type_Fctr.makeKey(roofType))
                                               .orElseThrow()
                                               .buildingGroup2();

                               BigDecimal roofTypeBuildings = (((classBRGroup1).multiply(roofTypeBulding1))
                                               .add((classBRGroup2)
                                                               .multiply(roofTypeBulding2)))
                                               .divide(classBaseRateGroups, 2, RoundingMode.HALF_EVEN);
                               log.info("roof type buildings: {}", roofTypeBuildings);

                               BigDecimal roofTypeGroups1 = tableRecordFetcher.getTable(Roof_Type_Fctr.class)
                                               .getRecord(Roof_Type_Fctr.makeKey(roofType))
                                               .orElseThrow()
                                               .contentsGroup1();

                               BigDecimal roofTypeGroups2 = tableRecordFetcher.getTable(Roof_Type_Fctr.class)
                                               .getRecord(Roof_Type_Fctr.makeKey(roofType))
                                               .orElseThrow()
                                               .contentsGroup2();

                               BigDecimal roofTypeContents = (((classBRContent1).multiply(roofTypeGroups1))
                                               .add((classBRContent2)
                                                               .multiply(roofTypeGroups2)))
                                               .divide(classBaseRateContents, 2, RoundingMode.HALF_EVEN);

                               log.info("roof type contents: {}", roofTypeContents);

                               String roofAge = building.data().roofAge();
                               log.info("roof age: {}", roofAge);

                               BigDecimal roofAgeBulding1 = tableRecordFetcher.getTable(Roof_Age_Fct.class)
                                               .getRecord(Roof_Age_Fct.makeKey(roofAge))
                                               .orElseThrow()
                                               .buildingGroup1();

                               BigDecimal roofAgeBulding2 = tableRecordFetcher.getTable(Roof_Age_Fct.class)
                                               .getRecord(Roof_Age_Fct.makeKey(roofAge))
                                               .orElseThrow()
                                               .buildingGroup2();

                               BigDecimal roofAgeBuildings = (((classBRContent1).multiply(roofAgeBulding1))
                                               .add((classBRContent2)
                                                               .multiply(roofAgeBulding2)))
                                               .divide(classBaseRateGroups, 2, RoundingMode.HALF_EVEN);
                               log.info("roof age buildings: {}", roofAgeBuildings);

                               BigDecimal roofAgeGroups1 = tableRecordFetcher.getTable(Roof_Age_Fct.class)
                                               .getRecord(Roof_Age_Fct.makeKey(roofAge))
                                               .orElseThrow()
                                               .contentsGroup1();

                               BigDecimal roofAgeGroups2 = tableRecordFetcher.getTable(Roof_Age_Fct.class)
                                               .getRecord(Roof_Age_Fct.makeKey(roofAge))
                                               .orElseThrow()
                                               .contentsGroup2();

                               BigDecimal roofAgeContents = (((classBRGroup1).multiply(roofAgeGroups1))
                                               .add((classBRGroup2)
                                                               .multiply(roofAgeGroups2)))
                                               .divide(classBaseRateContents, 2, RoundingMode.HALF_EVEN);

                               String roofACVValue = building.data().roofACV();
                               log.info("roof acv value: {}", roofACVValue);

                               BigDecimal roofACVFactor = tableRecordFetcher.getTable(RoofACV.class)
                                               .getRecord(RoofACV.makeKey(roofACVValue))
                                               .orElseThrow()
                                               .factor();

                               BigDecimal roofACVBuildings = (((classBRContent1).multiply(roofACVFactor))
                                               .add((classBRContent2)
                                                               .multiply(roofACVFactor)))
                                               .divide(classBaseRateGroups, 2, RoundingMode.HALF_EVEN);
                               log.info("roof ACV buildings: {}", roofACVBuildings);

                               BigDecimal roofACVContents = (((classBRContent1).multiply(roofACVFactor))
                                               .add((classBRContent2)
                                                               .multiply(roofACVFactor)))
                                               .divide(classBaseRateContents, 2, RoundingMode.HALF_EVEN);
                               log.info("roof ACV contents: {}", roofACVContents);

                               String hasSprinklers = building.data().propertySafeguards().hasSprinklers();
                               String protectDevices = "";
                               if (hasSprinklers.equals("Yes") && !building.data().propertySafeguards().fireSafeguards().contains("None") && !building.data().propertySafeguards().burglarSafeguards().contains("None")) {
                                       protectDevices = "3Systems";
                               } else if ((hasSprinklers.equals("Yes") && !building.data().propertySafeguards().fireSafeguards().contains("None")) ||
                                       (building.data().propertySafeguards().fireSafeguards().equals("Yes") && !building.data().propertySafeguards().burglarSafeguards().contains("None")) ||
                                       (!building.data().propertySafeguards().fireSafeguards().contains("None") && !building.data().propertySafeguards().burglarSafeguards().contains("None"))
                               ) {
                                       protectDevices = "2of3Systems";
                               } else if (hasSprinklers.equals("Yes")) {
                                       protectDevices = "SprinklerSystem";
                               } else if (!building.data().propertySafeguards().fireSafeguards().contains("None")) {
                                       protectDevices = "FireAlarmSystem";
                               } else if (!building.data().propertySafeguards().burglarSafeguards().contains("None")) {
                                       protectDevices = "BurglaryProtectiveSafeguards";
                               }

                               log.info("protective devices: {}", protectDevices);

                               BigDecimal safetyBulding1 = tableRecordFetcher.getTable(Safety_Factors.class)
                                               .getRecord(Safety_Factors.makeKey(protectDevices))
                                               .orElseThrow()
                                               .buildingGroup1();

                               BigDecimal safetyBulding2 = tableRecordFetcher.getTable(Safety_Factors.class)
                                               .getRecord(Safety_Factors.makeKey(protectDevices))
                                               .orElseThrow()
                                               .buildingGroup2();

                               BigDecimal safetyBuildings = (((classBRGroup1).multiply(safetyBulding1))
                                               .add((classBRGroup2)
                                                               .multiply(safetyBulding2)))
                                               .divide(classBaseRateGroups, 2, RoundingMode.HALF_EVEN);
                               log.info("safety buildings: {}", safetyBuildings);

                               BigDecimal safetyGroups1 = tableRecordFetcher.getTable(Safety_Factors.class)
                                               .getRecord(Safety_Factors.makeKey(protectDevices))
                                               .orElseThrow()
                                               .contentsGroup1();

                               BigDecimal safetyGroups2 = tableRecordFetcher.getTable(Safety_Factors.class)
                                               .getRecord(Safety_Factors.makeKey(protectDevices))
                                               .orElseThrow()
                                               .contentsGroup2();

                               BigDecimal safetyContents = (((classBRContent1).multiply(roofAgeGroups1))
                                               .add((classBRContent2)
                                                               .multiply(safetyGroups2)))
                                               .divide(classBaseRateContents, 2, RoundingMode.HALF_EVEN);
                               log.info("safety contents: {}", safetyContents);

                               BigDecimal buildingsRateProduct1 = (classBRGroup1.multiply(lcmGroup1)
                                               .multiply(formBuilding1).multiply(coinsuranceBuilding1)
                                               .multiply(deductibleBuilding1).multiply(whDeductibleBuilding1)
                                               .multiply(territory1).multiply(windHailExcFactor)
                                               .multiply(loiGroup1).multiply(protectionBulding1)
                                               .multiply(buildingCodeGradeBulding1)
                                               .multiply(buildingAgeBulding1).multiply(roofTypeBulding1)
                                               .multiply(roofAgeBulding1).multiply(roofACVFactor)
                                               .multiply(safetyBulding1)).setScale(3, RoundingMode.CEILING);

                               log.info("product of all building rates 1: {}", buildingsRateProduct1);
                               subtotalBuildingsRateProduct1 = subtotalBuildingsRateProduct1
                                               .add(buildingsRateProduct1);

                               BigDecimal buildingsRateProduct2 = (classBRGroup2.multiply(lcmGroup2)
                                               .multiply(formBuilding2).multiply(coinsuranceBuilding2)
                                               .multiply(deductibleBuilding2).multiply(whDeductibleBuilding2)
                                               .multiply(territory2).multiply(windHailExcFactor)
                                               .multiply(loiGroup2).multiply(protectionBulding2)
                                               .multiply(buildingCodeGradeBulding2)
                                               .multiply(buildingAgeBulding2).multiply(roofTypeBulding2)
                                               .multiply(roofAgeBulding2).multiply(roofACVFactor)
                                               .multiply(safetyBulding2)).setScale(3, RoundingMode.CEILING);

                               log.info("product of all building rates 2: {}", buildingsRateProduct2);
                               subtotalBuildingsRateProduct2 = subtotalBuildingsRateProduct2
                                               .add(buildingsRateProduct2);

                               BigDecimal buildingsRateProduct = (classBaseRateGroups.multiply(lcmBuildings)
                                               .multiply(formBuildings).multiply(coinsuranceBuildings)
                                               .multiply(deductibleBuildings).multiply(whDeductibleBuildings)
                                               .multiply(territoryBuildingRate).multiply(windHailRateBuildings)
                                               .multiply(loiBuildings).multiply(ppcBuildings)
                                               .multiply(buildingCodeGradeBuildings)
                                               .multiply(buildingAgeBuildings).multiply(roofTypeBuildings)
                                               .multiply(roofAgeBuildings).multiply(roofACVBuildings)
                                               .multiply(safetyBuildings)).setScale(3, RoundingMode.CEILING);
                               log.info("product of all building rates: {}", buildingsRateProduct);
                               subtotalRateBuildings = subtotalRateBuildings.add(buildingsRateProduct);

                               BigDecimal contentRateProduct1 = (classBRContent1.multiply(lcmContents1)
                                               .multiply(formContent1).multiply(coinsuranceContent1)
                                               .multiply(deductibleContents1).multiply(whDeductibleContents1)
                                               .multiply(territory1).multiply(windHailExcFactor)
                                               .multiply(loiContents1).multiply(protectionGroups1)
                                               .multiply(buildingCodeGradeGroups1)
                                               .multiply(buildingAgeGroups1).multiply(roofTypeGroups1)
                                               .multiply(roofAgeGroups1).multiply(roofACVContents)
                                               .multiply(safetyGroups1)).setScale(3, RoundingMode.CEILING);
                               log.info("product of content rates 1: {}", contentRateProduct1);
                               subtotalContentsRateProduct1 = subtotalContentsRateProduct1.add(contentRateProduct1);

                               BigDecimal contentRateProduct2 = (classBRContent2.multiply(lcmContents2)
                                               .multiply(formContent2).multiply(coinsuranceContent2)
                                               .multiply(deductibleContents2).multiply(whDeductibleContents2)
                                               .multiply(territory2).multiply(windHailExcFactor)
                                               .multiply(loiContents2).multiply(protectionGroups2)
                                               .multiply(buildingCodeGradeGroups2)
                                               .multiply(buildingAgeGroups2).multiply(roofTypeGroups2)
                                               .multiply(roofAgeGroups2).multiply(roofACVContents)
                                               .multiply(safetyGroups2)).setScale(3, RoundingMode.CEILING);
                               log.info("product of content rates 2: {}", contentRateProduct2);
                               subtotalContentsRateProduct2 = subtotalContentsRateProduct2.add(contentRateProduct2);

                               BigDecimal contentsRateProduct = (classBaseRateContents.multiply(lcmContents)
                                               .multiply(formContents).multiply(coinsuranceContents)
                                               .multiply(deductibleContents).multiply(whDeductibleContents)
                                               .multiply(territoryContentRate).multiply(windHailGroups)
                                               .multiply(loiContents).multiply(ppcContents)
                                               .multiply(buildingCodeGradeContents)
                                               .multiply(buildingAgeContents).multiply(roofTypeContents)
                                               .multiply(roofAgeContents).multiply(roofACVContents)
                                               .multiply(safetyContents)).setScale(3, RoundingMode.CEILING);
                               log.info("product of all content rates: {}", contentsRateProduct);
                               subtotalRateContents = subtotalRateContents.add(contentsRateProduct);

                               log.info("building 1 rates subtotal: {}", subtotalBuildingsRateProduct1);
                               log.info("building 2 rates subtotal: {}", subtotalBuildingsRateProduct2);
                               log.info("building rates subtotal: {}", subtotalRateBuildings);

                               log.info("contents 1 rates subtotal: {}", subtotalContentsRateProduct1);
                               log.info("contents 2 rates subtotal: {}", subtotalContentsRateProduct2);
                               log.info("contents rates subtotal: {}", subtotalRateContents);

                               int claimCount = 0;
                               if (quote.data().claimHistory() != null) {
                                       claimCount = quote.data().claimHistory().size();
                                }
                               log.info("claim count: {}", Integer.toString(claimCount));
                               BigDecimal claimsRate = tableRecordFetcher.getTable(PriorClaims.class)
                                               .getRecord(PriorClaims.makeKey(claimCount)).orElseThrow().premOpsFctr();
                               log.info("claim count: {}", claimsRate);

                               subtotalBuildingsRateProduct1 = subtotalBuildingsRateProduct1.multiply(claimsRate);
                               subtotalBuildingsRateProduct2 = subtotalBuildingsRateProduct2.multiply(claimsRate);
                               BigDecimal claimBuildingRate = (((classBRGroup1)
                                               .multiply(claimsRate))
                                               .add((classBRGroup2)
                                                               .multiply(claimsRate)))
                                               .divide(classBaseRateGroups, 2, RoundingMode.HALF_EVEN);
                               log.info("claim building rate: {}", claimBuildingRate);

                               subtotalContentsRateProduct1 = subtotalContentsRateProduct1.multiply(claimsRate);
                               subtotalContentsRateProduct2 = subtotalContentsRateProduct2.multiply(claimsRate);
                               BigDecimal claimContentsRate = (((classBRGroup1)
                                               .multiply(claimsRate))
                                               .add((classBRGroup2)
                                                               .multiply(claimsRate)))
                                               .divide(classBaseRateContents, 2, RoundingMode.HALF_EVEN);
                               log.info("claim contents rate: {}", claimContentsRate);

                               int businessCreditScore = quote.data().generalInformation().businessCreditScore();
                               int tierFactor = 0;
                               if (businessCreditScore >= 200 && businessCreditScore <= 555) {
                                       tierFactor = 1;
                               } else if (businessCreditScore >= 556 && businessCreditScore <= 635) {
                                       tierFactor = 2;
                               } else if (businessCreditScore >= 636 && businessCreditScore <= 689) {
                                       tierFactor = 3;
                               } else if (businessCreditScore >= 690 && businessCreditScore <= 726) {
                                       tierFactor = 4;
                               } else if (businessCreditScore >= 727 && businessCreditScore <= 762) {
                                       tierFactor = 5;
                               } else if (businessCreditScore >= 763 && businessCreditScore <= 791) {
                                       tierFactor = 6;
                               } else if (businessCreditScore >= 792 && businessCreditScore <= 820) {
                                       tierFactor = 7;
                               } else if (businessCreditScore >= 821 && businessCreditScore <= 853) {
                                       tierFactor = 8;
                               } else if (businessCreditScore >= 854 && businessCreditScore <= 900) {
                                       tierFactor = 9;
                               } else if (businessCreditScore >= 901 && businessCreditScore <= 997) {
                                       tierFactor = 10;
                               } else if (businessCreditScore == 998) {
                                       tierFactor = 98;
                               } else if (businessCreditScore == 999) {
                                       tierFactor = 99;
                               }

                               log.info("tier factor for business credit score: {}", tierFactor);

                               BigDecimal tierRateBuilding1 = tableRecordFetcher.getTable(Tier_Factors.class)
                                               .getRecord(Tier_Factors.makeKey(tierFactor)).orElseThrow()
                                               .buildingGroup1();
                               BigDecimal tierRateBuilding2 = tableRecordFetcher.getTable(Tier_Factors.class)
                                               .getRecord(Tier_Factors.makeKey(tierFactor)).orElseThrow()
                                               .buildingGroup2();
                               subtotalBuildingsRateProduct1 = subtotalBuildingsRateProduct1
                                               .multiply(tierRateBuilding1);
                               subtotalBuildingsRateProduct2 = subtotalBuildingsRateProduct2
                                               .multiply(tierRateBuilding2);
                               BigDecimal tierBuildingRate = (((classBRGroup1)
                                               .multiply(tierRateBuilding1))
                                               .add((classBRGroup2)
                                                               .multiply(tierRateBuilding2)))
                                               .divide(classBaseRateGroups, 2, RoundingMode.HALF_EVEN);
                               log.info("tier building rate: {}", tierBuildingRate);

                               BigDecimal tierRateContent1 = tableRecordFetcher.getTable(Tier_Factors.class)
                                               .getRecord(Tier_Factors.makeKey(tierFactor)).orElseThrow()
                                               .contentsGroup1();
                               BigDecimal tierRateContent2 = tableRecordFetcher.getTable(Tier_Factors.class)
                                               .getRecord(Tier_Factors.makeKey(tierFactor)).orElseThrow()
                                               .contentsGroup2();
                               subtotalContentsRateProduct1 = subtotalContentsRateProduct1.multiply(tierRateContent1);
                               subtotalContentsRateProduct2 = subtotalContentsRateProduct2.multiply(tierRateContent2);
                               BigDecimal tierContentsRate = (((classBRContent1)
                                               .multiply(tierRateContent1))
                                               .add((classBRContent2)
                                                               .multiply(tierRateContent2)))
                                               .divide(classBaseRateContents, 2, RoundingMode.HALF_EVEN);
                               log.info("tier contents rate: {}", tierContentsRate);

                               subtotalBuildingsRateProduct1 = subtotalBuildingsRateProduct1.multiply(buildingValue)
                                               .divide(oneHundredth);
                               subtotalBuildingsRateProduct2 = subtotalBuildingsRateProduct2.multiply(buildingValue)
                                               .divide(oneHundredth);

                               BigDecimal finalBuildingRate = subtotalBuildingsRateProduct1
                                               .add(subtotalBuildingsRateProduct2);

                               subtotalContentsRateProduct1 = subtotalContentsRateProduct1.multiply(contentValue)
                                               .divide(oneHundredth);
                               subtotalContentsRateProduct2 = subtotalContentsRateProduct2.multiply(contentValue)
                                               .divide(oneHundredth);

                               finalBuildingRate.add(subtotalContentsRateProduct1).add(subtotalContentsRateProduct2);
                               log.info("final building and contents premium: {}", finalBuildingRate);

                               buildingRate = buildingRate.add(finalBuildingRate);
                       }
               }

                // TODO: discount factors, tenure is for renewals only

                // affinity discount
                String affinityDiscount = quote.data().affinityDiscount();
                BigDecimal affinityRate = tableRecordFetcher.getTable(DiscountFactorsAffinity.class)
                                .getRecord(DiscountFactorsAffinity.makeKey(affinityDiscount)).orElseThrow().factor();

                // TODO: advance quote is difference between quote date and issuance date

                log.info("final premium for all locations and buildings: {}", buildingRate);

                // taxes
                BigDecimal allTaxes = new BigDecimal("0.00");
                BigDecimal carrierPolicyFeeAmount = new BigDecimal("0.00");
                BigDecimal stampingFeeAmount = new BigDecimal("0.00");
                BigDecimal brokerFeeAmount = new BigDecimal("0.00");
                BigDecimal installmentFeeAmount = new BigDecimal("0.00");
                BigDecimal taxAmounts = new BigDecimal("0.00");
                for (PropertyLocation loc : quote.propertyLocations()) {
                        BigDecimal oneHundredth = new BigDecimal("100.00");
                        state = loc.data().packageProductBasicInfo().locationAddress().state();
                        log.info("state: {}", state);

                        BigDecimal premiumRisk = tableRecordFetcher.getTable(TaxFeesProperty.class).getRecord(TaxFeesProperty.makeKey(state)).orElseThrow().minEP();
                        log.info("premium risk: {} ", premiumRisk.toString());
                        if (buildingRate.compareTo(premiumRisk) == 1) {
                                premiumRisk = buildingRate;
                        }
                        BigDecimal surplusLinesTax = (tableRecordFetcher.getTable(TaxFeesProperty.class).getRecord(TaxFeesProperty.makeKey(state)).orElseThrow().surplusLinesTax().divide(oneHundredth)).multiply(premiumRisk);
                        taxAmounts = taxAmounts.add(surplusLinesTax);
                        log.info("surplus lines tax: {} ", surplusLinesTax.toString());

                        BigDecimal miscTax = tableRecordFetcher.getTable(TaxFeesProperty.class).getRecord(TaxFeesProperty.makeKey(state)).orElseThrow().miscTax();
                        taxAmounts = taxAmounts.add(miscTax);
                        log.info("misc tax: {} ", miscTax.toString());

                        BigDecimal stampingFee = (tableRecordFetcher.getTable(TaxFeesProperty.class).getRecord(TaxFeesProperty.makeKey(state)).orElseThrow().stampingFee()).multiply(premiumRisk);
                        stampingFeeAmount = stampingFeeAmount.add(stampingFee);
                        log.info("stamping fee: {} ", stampingFee.toString());

                        int carrierPolicyFeeAsInt = tableRecordFetcher.getTable(TaxFeesProperty.class).getRecord(TaxFeesProperty.makeKey(state)).orElseThrow().policyFee();
                        BigDecimal carrierPolicyFee = BigDecimal.valueOf(carrierPolicyFeeAsInt);
                        carrierPolicyFeeAmount = carrierPolicyFeeAmount.add(carrierPolicyFee);
                        log.info("carrier policy fee: {} ", carrierPolicyFee.toString());

                        int brokerFeeAsInt = tableRecordFetcher.getTable(TaxFeesProperty.class).getRecord(TaxFeesProperty.makeKey(state)).orElseThrow().brokerFee();
                        BigDecimal brokerFee = BigDecimal.valueOf(brokerFeeAsInt);
                        brokerFeeAmount = brokerFeeAmount.add(brokerFee);
                        log.info("broker fee: {} ", brokerFee.toString());

                        BigDecimal installmentFee = ((tableRecordFetcher.getTable(TaxFeesProperty.class).getRecord(TaxFeesProperty.makeKey(state)).orElseThrow().installmentFee()).divide(oneHundredth)).multiply(premiumRisk);
                        installmentFeeAmount = installmentFeeAmount.add(installmentFee);
                        log.info("installment fee: {} ", installmentFee.toString());

                        BigDecimal currentTaxesFees = premiumRisk.add(surplusLinesTax).add(miscTax).add(stampingFee).add(brokerFee).add(installmentFee).add(carrierPolicyFee);
                        log.info("current tax + fee amount: {} ", currentTaxesFees.toString());

                        allTaxes = allTaxes.add(taxAmounts);
                        log.info("final tax amountt: {} ", allTaxes.toString());
                }

                BigDecimal minimumPremium = new BigDecimal("700.0");

                if (minimumPremium.compareTo(buildingRate) == 1) {
                    ratingItems.add(RatingItem.builder()
                            .elementLocator(quote.locator())
                            .chargeType(ChargeType.premium)
                            .rate(minimumPremium)
                            .build());
                    ratingItems.add(RatingItem.builder()
                            .elementLocator(quote.locator())
                            .chargeType(ChargeType.tax)
                            .rate(allTaxes)
                            .build());
                    ratingItems.add(RatingItem.builder()
                            .elementLocator(quote.locator())
                            .chargeType(ChargeType.carrierPolicyFee)
                            .rate(carrierPolicyFeeAmount)
                            .build());
                    ratingItems.add(RatingItem.builder()
                            .elementLocator(quote.locator())
                            .chargeType(ChargeType.stampingFee)
                            .rate(stampingFeeAmount)
                            .build());
                    ratingItems.add(RatingItem.builder()
                            .elementLocator(quote.locator())
                            .chargeType(ChargeType.brokerFee)
                            .rate(brokerFeeAmount)
                            .build());
                    ratingItems.add(RatingItem.builder()
                            .elementLocator(quote.locator())
                            .chargeType(ChargeType.installmentFee)
                            .rate(installmentFeeAmount)
                            .build());
                } else {
                    ratingItems.add(RatingItem.builder()
                            .elementLocator(quote.locator())
                            .chargeType(ChargeType.tax)
                            .rate(allTaxes)
                            .build());
                    ratingItems.add(RatingItem.builder()
                            .elementLocator(quote.locator())
                            .chargeType(ChargeType.carrierPolicyFee)
                            .rate(carrierPolicyFeeAmount)
                            .build());
                    ratingItems.add(RatingItem.builder()
                            .elementLocator(quote.locator())
                            .chargeType(ChargeType.stampingFee)
                            .rate(stampingFeeAmount)
                            .build());
                    ratingItems.add(RatingItem.builder()
                            .elementLocator(quote.locator())
                            .chargeType(ChargeType.brokerFee)
                            .rate(brokerFeeAmount)
                            .build());
                    ratingItems.add(RatingItem.builder()
                            .elementLocator(quote.locator())
                            .chargeType(ChargeType.installmentFee)
                            .rate(installmentFeeAmount)
                            .build());
                }
                log.info("rateInsured -> {}", ratingItems);

                RatingSet ratingSet = RatingSet.builder()
                                .ok(true)
                                .addRatingItems(ratingItems)
                                .build();
                log.info("rate -> {}", ratingSet);
                return ratingSet;
        }

}