package com.thinking.ticket.jpa;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.OBJECT_FACTORY_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

/* 조합 B: 같은 Feature를 Spring 컨텍스트 + 실제 MySQL(Testcontainers) + JPA 어댑터로 실행.
 * 조합 A(in-memory)와 동일한 features를 선택하되 glue만 다르다 — 아웃바운드 어댑터만 교체,
 * Core/Feature는 무수정. cucumber-spring의 SpringFactory로 스텝을 조립한다. */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.thinking.ticket.jpa")
@ConfigurationParameter(key = OBJECT_FACTORY_PROPERTY_NAME, value = "io.cucumber.spring.SpringFactory")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty, summary")
class JpaCucumberAcceptanceTest {
}
