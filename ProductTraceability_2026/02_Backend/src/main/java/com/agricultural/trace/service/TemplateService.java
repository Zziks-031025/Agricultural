package com.agricultural.trace.service;

import com.agricultural.trace.entity.TraceField;
import com.agricultural.trace.entity.TraceStage;
import com.agricultural.trace.entity.TraceTemplate;
import com.agricultural.trace.mapper.TraceFieldMapper;
import com.agricultural.trace.mapper.TraceStageMapper;
import com.agricultural.trace.mapper.TraceTemplateMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateService {

    private final TraceTemplateMapper templateMapper;
    private final TraceStageMapper stageMapper;
    private final TraceFieldMapper fieldMapper;

    public Page<TraceTemplate> getTemplateList(Integer current, Integer size, String templateName, Integer templateType) {
        LambdaQueryWrapper<TraceTemplate> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(templateName)) {
            wrapper.like(TraceTemplate::getTemplateName, templateName);
        }
        if (templateType != null) {
            wrapper.eq(TraceTemplate::getTemplateType, templateType);
        }
        wrapper.orderByAsc(TraceTemplate::getSort);
        return templateMapper.selectPage(new Page<>(current, size), wrapper);
    }

    public TraceTemplate getTemplateDetail(Long id) {
        TraceTemplate template = templateMapper.selectById(id);
        if (template == null) {
            throw new RuntimeException("Template not found");
        }
        return template;
    }

    public void addTemplate(TraceTemplate template) {
        LambdaQueryWrapper<TraceTemplate> check = new LambdaQueryWrapper<>();
        check.eq(TraceTemplate::getTemplateCode, template.getTemplateCode());
        if (templateMapper.selectCount(check) > 0) {
            throw new RuntimeException("Template code already exists");
        }
        template.setCreateTime(LocalDateTime.now());
        template.setUpdateTime(LocalDateTime.now());
        templateMapper.insert(template);
        log.info("Template created, code={}", template.getTemplateCode());
    }

    public void updateTemplate(TraceTemplate template) {
        TraceTemplate existing = templateMapper.selectById(template.getId());
        if (existing == null) {
            throw new RuntimeException("Template not found");
        }
        existing.setTemplateName(template.getTemplateName());
        existing.setTemplateType(template.getTemplateType());
        existing.setProductCategory(template.getProductCategory());
        existing.setDescription(template.getDescription());
        existing.setSort(template.getSort());
        existing.setRemark(template.getRemark());
        existing.setUpdateTime(LocalDateTime.now());
        templateMapper.updateById(existing);
        log.info("Template updated, id={}", template.getId());
    }

    @Transactional
    public void deleteTemplate(Long id) {
        TraceTemplate template = templateMapper.selectById(id);
        if (template == null) {
            throw new RuntimeException("Template not found");
        }
        LambdaQueryWrapper<TraceStage> stageQuery = new LambdaQueryWrapper<>();
        stageQuery.eq(TraceStage::getTemplateId, id);
        List<TraceStage> stages = stageMapper.selectList(stageQuery);
        for (TraceStage stage : stages) {
            LambdaQueryWrapper<TraceField> fieldQuery = new LambdaQueryWrapper<>();
            fieldQuery.eq(TraceField::getStageId, stage.getId());
            fieldMapper.delete(fieldQuery);
        }
        stageMapper.delete(stageQuery);
        templateMapper.deleteById(id);
        log.info("Template deleted with cascade data, id={}", id);
    }

    public void toggleTemplateStatus(Long id, Integer status) {
        TraceTemplate template = templateMapper.selectById(id);
        if (template == null) {
            throw new RuntimeException("Template not found");
        }
        template.setStatus(status);
        template.setUpdateTime(LocalDateTime.now());
        templateMapper.updateById(template);
        log.info("Template status updated, id={}, status={}", id, status);
    }

    public List<TraceStage> getStageList(Long templateId) {
        LambdaQueryWrapper<TraceStage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TraceStage::getTemplateId, templateId);
        wrapper.orderByAsc(TraceStage::getSort);
        return stageMapper.selectList(wrapper);
    }

    public void addStage(TraceStage stage) {
        stage.setCreateTime(LocalDateTime.now());
        stage.setUpdateTime(LocalDateTime.now());
        stageMapper.insert(stage);
        log.info("Stage created, templateId={}, stageName={}", stage.getTemplateId(), stage.getStageName());
    }

    public void updateStage(TraceStage stage) {
        TraceStage existing = stageMapper.selectById(stage.getId());
        if (existing == null) {
            throw new RuntimeException("Stage not found");
        }
        existing.setStageName(stage.getStageName());
        existing.setStageCode(stage.getStageCode());
        existing.setStageType(stage.getStageType());
        existing.setSort(stage.getSort());
        existing.setIsRequired(stage.getIsRequired());
        existing.setDescription(stage.getDescription());
        existing.setUpdateTime(LocalDateTime.now());
        stageMapper.updateById(existing);
        log.info("Stage updated, id={}", stage.getId());
    }

    @Transactional
    public void deleteStage(Long id) {
        TraceStage stage = stageMapper.selectById(id);
        if (stage == null) {
            throw new RuntimeException("Stage not found");
        }
        LambdaQueryWrapper<TraceField> fieldQuery = new LambdaQueryWrapper<>();
        fieldQuery.eq(TraceField::getStageId, id);
        fieldMapper.delete(fieldQuery);
        stageMapper.deleteById(id);
        log.info("Stage deleted with fields, id={}", id);
    }

    public List<TraceField> getFieldList(Long stageId) {
        LambdaQueryWrapper<TraceField> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TraceField::getStageId, stageId);
        wrapper.orderByAsc(TraceField::getSort);
        return fieldMapper.selectList(wrapper);
    }

    public void addField(TraceField field) {
        field.setCreateTime(LocalDateTime.now());
        field.setUpdateTime(LocalDateTime.now());
        fieldMapper.insert(field);
        log.info("Field created, stageId={}, fieldName={}", field.getStageId(), field.getFieldName());
    }

    public void updateField(TraceField field) {
        TraceField existing = fieldMapper.selectById(field.getId());
        if (existing == null) {
            throw new RuntimeException("Field not found");
        }
        existing.setFieldName(field.getFieldName());
        existing.setFieldCode(field.getFieldCode());
        existing.setFieldType(field.getFieldType());
        existing.setFieldOptions(field.getFieldOptions());
        existing.setIsRequired(field.getIsRequired());
        existing.setSort(field.getSort());
        existing.setPlaceholder(field.getPlaceholder());
        existing.setDefaultValue(field.getDefaultValue());
        existing.setValidationRule(field.getValidationRule());
        existing.setUpdateTime(LocalDateTime.now());
        fieldMapper.updateById(existing);
        log.info("Field updated, id={}", field.getId());
    }

    public void deleteField(Long id) {
        TraceField field = fieldMapper.selectById(id);
        if (field == null) {
            throw new RuntimeException("Field not found");
        }
        fieldMapper.deleteById(id);
        log.info("Field deleted, id={}", id);
    }

    public List<Map<String, Object>> getSystemFieldCatalog() {
        List<Map<String, Object>> catalog = new ArrayList<>();

        catalog.add(buildStageCatalog(
                "batch_init",
                "Batch Init",
                1,
                "trace_batch",
                Arrays.asList(
                        buildFieldCatalog("batchCode", "Batch Code", "text", "trace_batch.batch_code", "Unique batch identifier"),
                        buildFieldCatalog("productName", "Product Name", "text", "trace_batch.product_name", "Main product name"),
                        buildFieldCatalog("breed", "Breed", "text", "trace_batch.breed", "Breed or variety"),
                        buildFieldCatalog("initQuantity", "Initial Quantity", "number", "trace_batch.init_quantity", "Quantity recorded at batch creation"),
                        buildFieldCatalog("unit", "Unit", "text", "trace_batch.unit", "Quantity unit"),
                        buildFieldCatalog("originLocation", "Origin Location", "text", "trace_batch.origin_location", "Human-readable origin text"),
                        buildFieldCatalog("seedSource", "Seed Source", "text", "trace_batch.seed_source", "Seedling or seed source"),
                        buildFieldCatalog("manager", "Manager", "text", "trace_batch.manager", "Responsible operator"),
                        buildFieldCatalog("productionDate", "Production Date", "date", "trace_batch.production_date", "Production/start date"),
                        buildFieldCatalog("expectedHarvestDate", "Expected Harvest Date", "date", "trace_batch.expected_harvest_date", "Expected harvest/outbound date"),
                        buildFieldCatalog("latitude", "Latitude", "number", "trace_batch.latitude", "Geo latitude"),
                        buildFieldCatalog("longitude", "Longitude", "number", "trace_batch.longitude", "Geo longitude")
                )
        ));

        catalog.add(buildStageCatalog(
                "growth",
                "Growth Record",
                2,
                "trace_record",
                Arrays.asList(
                        buildFieldCatalog("recordDate", "Record Date", "date", "trace_record.record_date", "Date of feeding or inspection"),
                        buildFieldCatalog("recordType", "Record Type", "select", "trace_record.record_type", "feeding, vaccine, inspect and similar"),
                        buildFieldCatalog("itemName", "Item Name", "text", "trace_record.item_name", "Feed, vaccine or material name"),
                        buildFieldCatalog("amount", "Amount", "number", "trace_record.amount", "Usage amount"),
                        buildFieldCatalog("description", "Description", "text", "trace_record.description", "Operation description"),
                        buildFieldCatalog("location", "Location", "text", "trace_record.location", "Work location"),
                        buildFieldCatalog("operator", "Operator", "text", "trace_record.operator", "Responsible operator"),
                        buildFieldCatalog("images", "Images", "image", "trace_record.images", "On-site record images")
                )
        ));

        catalog.add(buildStageCatalog(
                "inspection",
                "Inspection",
                4,
                "trace_inspection",
                Arrays.asList(
                        buildFieldCatalog("inspectionDate", "Inspection Date", "date", "trace_inspection.inspection_date", "Inspection execution date"),
                        buildFieldCatalog("checkResult", "Check Result", "select", "trace_inspection.check_result", "1 = pass, 0 = fail"),
                        buildFieldCatalog("inspectionItems", "Inspection Items", "text", "trace_inspection.inspection_items", "Inspection item summary"),
                        buildFieldCatalog("inspector", "Inspector", "text", "trace_inspection.inspector", "Inspector name"),
                        buildFieldCatalog("inspectorCode", "Inspector Code", "text", "trace_inspection.inspector_code", "Inspector certificate code"),
                        buildFieldCatalog("certNo", "Certificate No", "text", "trace_inspection.cert_no", "Certificate identifier"),
                        buildFieldCatalog("certImage", "Certificate Image", "image", "trace_inspection.cert_image", "Certificate scan or photo"),
                        buildFieldCatalog("remark", "Remark", "text", "trace_inspection.remark", "Extra inspection note")
                )
        ));

        catalog.add(buildStageCatalog(
                "processing",
                "Processing",
                3,
                "trace_processing",
                Arrays.asList(
                        buildFieldCatalog("processingDate", "Processing Date", "date", "trace_processing.processing_date", "Processing execution date"),
                        buildFieldCatalog("processMethod", "Process Method", "text", "trace_processing.process_method", "Cleaning, cutting, packaging and similar"),
                        buildFieldCatalog("inputQuantity", "Input Quantity", "number", "trace_processing.input_quantity", "Input material quantity"),
                        buildFieldCatalog("inputUnit", "Input Unit", "text", "trace_processing.input_unit", "Input quantity unit"),
                        buildFieldCatalog("outputQuantity", "Output Quantity", "number", "trace_processing.output_quantity", "Processed output quantity"),
                        buildFieldCatalog("outputUnit", "Output Unit", "text", "trace_processing.output_unit", "Output quantity unit"),
                        buildFieldCatalog("specs", "Specs", "text", "trace_processing.specs", "Package or specification description"),
                        buildFieldCatalog("operator", "Operator", "text", "trace_processing.operator", "Responsible operator"),
                        buildFieldCatalog("images", "Images", "image", "trace_processing.images", "Processing images")
                )
        ));

        catalog.add(buildStageCatalog(
                "storage",
                "Storage",
                5,
                "trace_storage",
                Arrays.asList(
                        buildFieldCatalog("storageDate", "Storage Date", "date", "trace_storage.storage_date", "Inbound or outbound date"),
                        buildFieldCatalog("storageType", "Storage Type", "select", "trace_storage.storage_type", "Inbound, outbound or stock check"),
                        buildFieldCatalog("warehouseName", "Warehouse Name", "text", "trace_storage.warehouse_name", "Warehouse or zone name"),
                        buildFieldCatalog("warehouseLocation", "Warehouse Location", "text", "trace_storage.warehouse_location", "Warehouse address or location text"),
                        buildFieldCatalog("storageQuantity", "Storage Quantity", "number", "trace_storage.storage_quantity", "Storage quantity"),
                        buildFieldCatalog("temperature", "Temperature", "number", "trace_storage.temperature", "Warehouse temperature"),
                        buildFieldCatalog("humidity", "Humidity", "number", "trace_storage.humidity", "Warehouse humidity"),
                        buildFieldCatalog("storageCondition", "Storage Condition", "text", "trace_storage.storage_condition", "Cold, ambient and similar"),
                        buildFieldCatalog("operator", "Operator", "text", "trace_storage.operator", "Responsible operator"),
                        buildFieldCatalog("images", "Images", "image", "trace_storage.images", "Warehouse images")
                )
        ));

        catalog.add(buildStageCatalog(
                "transport",
                "Transport",
                6,
                "trace_transport",
                Arrays.asList(
                        buildFieldCatalog("transportDate", "Transport Date", "date", "trace_transport.transport_date", "Shipment date"),
                        buildFieldCatalog("logisticsNo", "Logistics No", "text", "trace_transport.logistics_no", "Logistics order number"),
                        buildFieldCatalog("plateNumber", "Plate Number", "text", "trace_transport.plate_number", "Vehicle plate number"),
                        buildFieldCatalog("driverName", "Driver Name", "text", "trace_transport.driver_name", "Driver name"),
                        buildFieldCatalog("driverPhone", "Driver Phone", "text", "trace_transport.driver_phone", "Driver contact phone"),
                        buildFieldCatalog("receiverName", "Receiver Name", "text", "trace_transport.receiver_name", "Receiver enterprise or contact"),
                        buildFieldCatalog("departureLocation", "Departure Location", "text", "trace_transport.departure_location", "Shipment origin"),
                        buildFieldCatalog("destination", "Destination", "text", "trace_transport.destination", "Shipment destination"),
                        buildFieldCatalog("transportQuantity", "Transport Quantity", "number", "trace_transport.transport_quantity", "Transport quantity"),
                        buildFieldCatalog("temperature", "Temperature", "number", "trace_transport.temperature", "Cabin temperature"),
                        buildFieldCatalog("humidity", "Humidity", "number", "trace_transport.humidity", "Cabin humidity"),
                        buildFieldCatalog("images", "Images", "image", "trace_transport.images", "Transport proof images")
                )
        ));

        catalog.add(buildStageCatalog(
                "sale",
                "Sale",
                7,
                "trace_sale",
                Arrays.asList(
                        buildFieldCatalog("saleDate", "Sale Date", "date", "trace_sale.sale_date", "Sale date"),
                        buildFieldCatalog("buyerName", "Buyer Name", "text", "trace_sale.buyer_name", "Buyer or customer name"),
                        buildFieldCatalog("saleChannel", "Sale Channel", "text", "trace_sale.sale_channel", "Store, ecommerce or distribution"),
                        buildFieldCatalog("saleQuantity", "Sale Quantity", "number", "trace_sale.sale_quantity", "Sold quantity"),
                        buildFieldCatalog("salePrice", "Sale Price", "number", "trace_sale.sale_price", "Unit price"),
                        buildFieldCatalog("totalAmount", "Total Amount", "number", "trace_sale.total_amount", "Total sale amount"),
                        buildFieldCatalog("destination", "Destination", "text", "trace_sale.destination", "Sale destination"),
                        buildFieldCatalog("saleVoucher", "Sale Voucher", "image", "trace_sale.sale_voucher", "Invoice or delivery voucher"),
                        buildFieldCatalog("remark", "Remark", "text", "trace_sale.remark", "Extra sale note")
                )
        ));

        return catalog;
    }

    private Map<String, Object> buildStageCatalog(String stageCode,
                                                  String stageName,
                                                  Integer stageType,
                                                  String sourceTable,
                                                  List<Map<String, Object>> fields) {
        Map<String, Object> catalog = new LinkedHashMap<>();
        catalog.put("stageCode", stageCode);
        catalog.put("stageName", stageName);
        catalog.put("stageType", stageType);
        catalog.put("sourceTable", sourceTable);
        catalog.put("fields", fields);
        return catalog;
    }

    private Map<String, Object> buildFieldCatalog(String fieldCode,
                                                  String fieldName,
                                                  String fieldType,
                                                  String sourceColumn,
                                                  String description) {
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("fieldCode", fieldCode);
        field.put("fieldName", fieldName);
        field.put("fieldType", fieldType);
        field.put("sourceColumn", sourceColumn);
        field.put("description", description);
        return field;
    }
}
