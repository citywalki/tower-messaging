---
title: Add getCommandClass method to HandlerRegistry
---

## Initial User Prompt

在 HandlerRegistry 中增加 getCommandClass(String name) 方法，支持通过名称查询 Command Class。使用 @CommandName 注解显式指定名称，无注解时自动派生（去掉 Command 后缀）。需要在 messaging-core 模块中新增注解、更新 HandlerRegistry 接口和 DefaultHandlerRegistry 实现。

## Description

// Will be filled in future stages by business analyst
