package com.agricultural.assistplatform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.agricultural.assistplatform.mapper")
public class AgriculturalApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgriculturalApplication.class, args);
    }

}
