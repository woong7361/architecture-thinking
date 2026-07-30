package com.thinking.ticket.e2e;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.OBJECT_FACTORY_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

/* HTTP 관통 구성: 같은 Feature를 HTTP -> 컨트롤러 -> 포트 -> Core -> 어댑터 -> 실제 MySQL 로 실행한다.
 * 다른 구성들과 Feature도 시나리오도 같고 glue만 다르다 — 인바운드 진입 슬롯만 실물로 바뀐 차이다. */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.thinking.ticket.e2e")
@ConfigurationParameter(key = OBJECT_FACTORY_PROPERTY_NAME, value = "io.cucumber.spring.SpringFactory")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty, summary")
class E2eCucumberAcceptanceTest {
}
