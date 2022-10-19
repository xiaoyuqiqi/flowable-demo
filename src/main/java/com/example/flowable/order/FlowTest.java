package com.example.flowable.order;

import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.impl.cfg.StandaloneProcessEngineConfiguration;

public class FlowTest {
    public static void main(String[] args) {
        ProcessEngineConfiguration cfg = new StandaloneProcessEngineConfiguration()
                /*.setJdbcUrl("jdbc:h2:mem:flowable;DB_CLOSE_DELAY=-1")
                .setJdbcUsername("sa")
                .setJdbcPassword("")
                .setJdbcDriver("org.h2.Driver")*/
                .setJdbcUrl("jdbc:mysql://127.0.0.1:3306/flowable?autoReconnect=true&useUnicode=true&characterEncoding=utf-8&serverTimezone=GMT%2B8&nullCatalogMeansCurrent=true")
                .setJdbcUsername("root")
                .setJdbcPassword("root")
                .setJdbcDriver("com.mysql.jdbc.Driver")
                //如果数据表不存在的时候，自动创建数据表
                .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
        // 执行完成后，就可以开始创建我们的流程引擎了
        ProcessEngine processEngine = cfg.buildProcessEngine();
       /* TaskService taskService = processEngine.getTaskService();
        //通过角色查询任务
        List<Task> tasks = taskService.createTaskQuery().taskCandidateGroup("managers").list();
        //通过用户名查询任务
        Task task = taskService.createTaskQuery().taskAssignee("ligang").singleResult();
        System.out.println(task.getName());
        task = taskService.createTaskQuery().processDefinitionId("holidayRequest:1:3").singleResult();
        System.out.println(task.getName());
        task = taskService.createTaskQuery().processInstanceId("4").singleResult();
        System.out.println(task.getName());
        task = taskService.createTaskQuery().processDefinitionId("holidayRequest:1:3").processInstanceId("4").taskAssignee("ligang").singleResult();
        System.out.println(task.getName());*/
        //获取流程定义id
        RepositoryService repositoryService = processEngine.getRepositoryService();


    }
}
