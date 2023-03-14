package com.stibo.demo.report.service;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

import org.springframework.stereotype.Service;

import com.stibo.demo.report.logging.LogTime;
import com.stibo.demo.report.model.Attribute;
import com.stibo.demo.report.model.AttributeGroup;
import com.stibo.demo.report.model.AttributeLink;
import com.stibo.demo.report.model.Category;
import com.stibo.demo.report.model.Datastandard;

@Service
public class ReportService {

    @LogTime
    public Stream<Stream<String>> report(Datastandard datastandard, String categoryId) {
    	if (datastandard == null || datastandard.getCategories() == null
    			|| datastandard.getAttributes() == null || datastandard.getAttributeGroups() == null) {
    		return Stream.empty();
    	}
    	
    	var categoryMap = prepareCategoryMap(datastandard.getCategories());
    	var attributeMap = prepareAttributeMap(datastandard.getAttributes());
    	var attributeGroupMap = prepareAttributeGroupMap(datastandard.getAttributeGroups());

    	var headerStream = Stream.of(includeHeader());
    	var dataStream = categoryStream(categoryId, categoryMap)
    						.flatMap(c -> transformCategory(c, attributeMap, attributeGroupMap));    	
    	return Stream.concat(headerStream, dataStream);
    }

    private Stream<String> includeHeader() {
    	return Stream.of("Category Name", "Attribute Name", "Description", "Type", "Groups");
    }
    
    private Stream<Stream<String>> transformCategory(Category category, Map<String, Attribute> attrMap,
    								Map<String, AttributeGroup> attrGroupMap) {
    	return category.getAttributeLinks()
			    		.stream()
			    		.map(al -> transformAttributeLink(category, 
			    											al, 
			    											attrMap, 
			    											attrGroupMap));
    }
    
    private Stream<String> transformAttributeLink(Category cat, AttributeLink attrLink, 
    										Map<String, Attribute> attrMap,
    										Map<String, AttributeGroup> attrGroupMap) {
    	Attribute attr = attrMap.get(attrLink.getId());
    	return Stream.of(
    				cat.getName(),
    				attributeName(attr, attrLink),
    				attributeDesc(attr),
    				attributeType(attrLink, attr, attrMap),
    				attributeGroups(attr, attrGroupMap)
    			);
    }
    
    private String attributeName(Attribute attribute, AttributeLink attrLink) {
    	return attribute.getName() + 
    			(attrLink.getOptional() == Boolean.TRUE ? "" : "*");
    }
    
    private String attributeDesc(Attribute attribute) {
    	return attribute.getDescription() == null || attribute.getDescription().isBlank() ?
    			"" : attribute.getDescription();
    }
    
    private String attributeType(AttributeLink attrLink, Attribute linkedAttr, Map<String, Attribute> attrMap) {
    	if (isCompositeAttribute(linkedAttr)) {
    		return compositeAttributeType(attrLink, linkedAttr, attrMap);
    	} else {
    		return basicAttributeType(linkedAttr);
    	}
    }
    
    private String compositeAttributeType(AttributeLink attrLink, Attribute linkedAttr, Map<String, Attribute> attrMap) {
    	StringBuilder sb = new StringBuilder();
    	sb.append(linkedAttr.getType().getId());
    	sb.append("{\n");
    	for (AttributeLink aLink : linkedAttr.getAttributeLinks()) {
    		Attribute attr = attrMap.get(aLink.getId());
    		sb.append("  ");
    		sb.append(attributeName(attr, aLink));
    		sb.append(": ");
    		
    		if (isCompositeAttribute(attr)) {
    			sb.append(compositeAttributeType(aLink, attr, attrMap));
    		} else {
    			sb.append(basicAttributeType(attr));
    		}
    		sb.append("\n");
    	}
    	sb.append("}");
    	sb.append(checkMultiValueFormat(linkedAttr));
    	return sb.toString();
    }
    
    private String attributeGroups(Attribute attribute, Map<String, AttributeGroup> attrGroupMap) {
    	return attribute.getGroupIds()
    				.stream()
    				.map(attrGroupMap::get)
    				.map(AttributeGroup::getName)
    				.collect(joining("\n"));
    }
    
    private String basicAttributeType(Attribute attr) {
    	return String.format("%s%s", attr.getType().getId(), checkMultiValueFormat(attr));
    }
    
    private String checkMultiValueFormat(Attribute attr) {
    	return attr.getType().getMultiValue() == Boolean.TRUE ? "[]" : "";
    }
    
    private boolean isCompositeAttribute(Attribute attr) {
    	return attr.getAttributeLinks() != null && attr.getAttributeLinks().size() > 0;
    }
    
    private Stream<Category> categoryStream(String categoryId, Map<String, Category> categoryMap) {
    	Builder<Category> catStream = Stream.<Category>builder();
    	Category category = categoryMap.get(categoryId);
    	while (category != null) {
    		catStream.accept(category);
    		category = category.getParentId() != null ? 
    						categoryMap.get(category.getParentId()) : null;
    	}
    	return catStream.build();
    }
    
    private Map<String, Category> prepareCategoryMap(List<Category> categories) {
		return categories.stream()
					.collect(toMap(Category::getId, identity()));
	}
	
	private Map<String, Attribute> prepareAttributeMap(List<Attribute> attributes) {
		return attributes.stream()
					.collect(toMap(Attribute::getId, identity()));
	}
	
	private Map<String, AttributeGroup> prepareAttributeGroupMap(List<AttributeGroup> attributes) {
		return attributes.stream()
					.collect(toMap(AttributeGroup::getId, identity()));
	}
}
