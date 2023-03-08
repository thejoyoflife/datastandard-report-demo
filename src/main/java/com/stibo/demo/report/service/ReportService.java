package com.stibo.demo.report.service;

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

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.joining;
import static java.util.function.Function.identity;

@Service
public class ReportService {

    @LogTime
    public Stream<Stream<String>> report(Datastandard datastandard, String categoryId) {
    	var categoryMap = prepareCategoryMap(datastandard.getCategories());
    	var attributeMap = prepareAttributeMap(datastandard.getAttributes());
    	var attributeGroupMap = prepareAttributeGroupMap(datastandard.getAttributeGroups());
    	
    	return categoryStream(categoryId, categoryMap)
    			.flatMap(c -> transformCategory(c, attributeMap, attributeGroupMap));    	
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
    				attributeType(attr),
    				attributeGroups(attr, attrGroupMap)
    			);
    }
    
    private String attributeName(Attribute attr, AttributeLink aLink) {
    	return attr.getName() + 
    			(aLink.getOptional() == Boolean.TRUE ? "" : "*");
    }
    
    private String attributeDesc(Attribute attr) {
    	return attr.getDescription() == null || attr.getDescription().isBlank() ?
    			"" : attr.getDescription();
    }
    
    private String attributeType(Attribute attr) {
    	return attr.getType().getId() + 
    			(attr.getType().getMultiValue() == Boolean.TRUE ? "[]" : "");
    }
    
    private String attributeGroups(Attribute attr, Map<String, AttributeGroup> attrGroupMap) {
    	return attr.getGroupIds()
    				.stream()
    				.map(agid -> attrGroupMap.get(agid))
    				.map(ag -> ag.getName())
    				.collect(joining("\n"));
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
