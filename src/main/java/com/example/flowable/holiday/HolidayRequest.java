package com.example.flowable.holiday;

import org.flowable.engine.*;
import org.flowable.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.task.Comment;
import org.flowable.task.api.Task;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
* 此类描述的是：测试demo
* 参考flowable手册https://tkjohn.github.io/flowable-userguide/
*/
public class HolidayRequest {
    public static void main(String[] args) {
        //1.创建了一个独立(standalone)配置
        ProcessEngineConfiguration cfg = new StandaloneProcessEngineConfiguration()
                /*.setJdbcUrl("jdbc:h2:mem:flowable;DB_CLOSE_DELAY=-1")
                .setJdbcUsername("sa")
                .setJdbcPassword("")
                .setJdbcDriver("org.h2.Driver")*/
                .setJdbcUrl("jdbc:mysql://192.168.0.137:3306/fwf_xyqq_flowable?autoReconnect=true&useUnicode=true&characterEncoding=utf-8&serverTimezone=GMT%2B8&nullCatalogMeansCurrent=true")
                .setJdbcUsername("root")
                .setJdbcPassword("123456")
                .setJdbcDriver("com.mysql.jdbc.Driver")
                //如果数据表不存在的时候，自动创建数据表
                .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
        //2.创建流程引擎
        ProcessEngine processEngine = cfg.buildProcessEngine();
        // 使用BPMN 2.0定义process。存储为XML，同时也是可以可视化的。NPMN 2.0标准可以让技术人员与业务人员都参与讨论业务流程中来

        //3.利用流程引擎部署流程
        RepositoryService repositoryService = processEngine.getRepositoryService();
        Deployment deployment = repositoryService.createDeployment()
                .addClasspathResource("holiday-request.bpmn20.xml")
                .deploy();
        //4.根据流程部署实例id获取流程定义实例
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deployment.getId())
                .singleResult();
        System.out.println("Found process definition : " + processDefinition.getName());

        //5.启动process实例
        //5.1需要一些初始化的变量，这里我们简单的从Scanner中获取，一般为web前端页面填写发起流程的表单，数据传到后端
        //5.1.1 scanner输入类似于web前端表单输入
        Scanner scanner= new Scanner(System.in);

        System.out.println("Who are you?");
        String employee = scanner.nextLine();

        System.out.println("How many holidays do you want to request?");
        Integer nrOfHolidays = Integer.valueOf(scanner.nextLine());

        System.out.println("Why do you need them?");
        String description = scanner.nextLine();
        //5.1.2 此处代码类似web后端获取前端表单传来的字段
        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("employee", employee);
        variables.put("nrOfHolidays", nrOfHolidays);
        variables.put("description", description);

        //6.发起流程
        RuntimeService runtimeService = processEngine.getRuntimeService();
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("holidayRequest", variables);

        //7.经理查询待办任务
        TaskService taskService = processEngine.getTaskService();
        /*将第一个任务指派给"经理(managers)"组，而第二个用户任务指派给请假申请的提交人。因此需要为第一个任务添加candidateGroups属性：
            <userTask id="approveTask" name="Approve or reject request" flowable:candidateGroups="managers"/>
          这里的candidateGroups，中文一般成为候选组，实际是一组用户的带号，在实际使用中可以写用户的角色id，或者组织机构（岗位、职位）id等
          当用户查看待办列表时，后台根据用户的id查询角色id，然后用角色id替换managers即可查询到对应的任务列表
         */
        List<Task> tasks = taskService.createTaskQuery().taskCandidateGroup("managers").list();
        //8.经理处理审批任务
        //实际一般为用户在web前端页面的待办任务列表
        System.out.println("You have " + tasks.size() + " tasks:");
        for (int i=0; i<tasks.size(); i++) {
            System.out.println((i+1) + ") " + tasks.get(i).getName());
        }
        System.out.println("Which task would you like to complete?");
        //8.1 选择要处理的任务
        //此处一般为web前端页面待办任务列表选择需要操作的任务
        int taskIndex = Integer.valueOf(scanner.nextLine());
        Task task = tasks.get(taskIndex - 1);
        //使用任务Id获取特定流程实例的变量
        Map<String, Object> processVariables = taskService.getVariables(task.getId());
        System.out.println(processVariables.get("employee") + " wants " +
                processVariables.get("nrOfHolidays") + " of holidays. Do you approve this?");
        //8.2 编写任务处理意见，并进行approved or rejected的操作
        //此处一般为web前端页面，点击“同意”或“驳回”的按钮，同意为"y",驳回为"n"
        boolean approved = scanner.nextLine().toLowerCase().equals("y");
        //8.3 对前端传来的审批意见进行保存
        //注意先保存意见，再完成任务，否则任务完成后找不到
        if (approved){
            Comment comment = taskService.addComment(task.getId(), processInstance.getProcessInstanceId(), "同意休假");
            comment.setUserId("manager");
            taskService.saveComment(comment);
        }else{
            Comment comment = taskService.addComment(task.getId(), processInstance.getProcessInstanceId(), "不允许休假");
            comment.setUserId("manager");
            taskService.saveComment(comment);
        }
        //8.4 完成任务
        //此处代码一般写在web后端，接收到前端的同意或驳回的参数，完成任务
        variables = new HashMap<String, Object>();
        variables.put("approved", approved);
        taskService.complete(task.getId(), variables);

        //9.申请人查询待办事务
        /*并如下所示为第二个任务添加assignee属性。请注意我们没有像上面的’managers’一样使用静态值，而是使用一个流程变量动态指派。这个流程变量是在流程实例启动时传递的：
            <userTask id="holidayApprovedTask" name="Holiday approved" flowable:assignee="${employee}"/>
         */
        //注意发起流程时的employee，最好为申请人或者审批人的id或其他唯一字段
        tasks = taskService.createTaskQuery().taskAssignee(employee).list();
        //10.申请人处理休假任务
        System.out.println("You have " + tasks.size() + " tasks:");
        for (int i=0; i<tasks.size(); i++) {
            System.out.println((i+1) + ") " + tasks.get(i).getName());
        }
        if (tasks.size() > 0){
            System.out.println("Which task would you like to complete?");
            //10.1 选择要处理的任务
            taskIndex = Integer.valueOf(scanner.nextLine());
            task = tasks.get(taskIndex - 1);
            //使用任务Id获取特定流程实例的变量
            processVariables = taskService.getVariables(task.getId());
            System.out.println("Hi " + processVariables.get("employee") + "please use your holidays !");
            //10.2 保存前端传来的审批意见
            Comment comment1 = taskService.addComment(task.getId(), processInstance.getProcessInstanceId(), processVariables.get("employee") + "休假中");
            comment1.setUserId("employe");
            taskService.saveComment(comment1);
            //10.3 完成任务
            variables = new HashMap<String, Object>();
            taskService.complete(task.getId(), variables);
        }
        //展示审批意见
        List<Comment> taskComments = taskService.getProcessInstanceComments(processInstance.getProcessInstanceId());
        for (Comment comment:taskComments){
            System.out.println("审批人："+comment.getUserId()
             + ",审批意见：" + comment.getFullMessage() + ",审批时间：" + comment.getTime());
        }

        //展示流程流转的历史记录
        /*HistoryService historyService = processEngine.getHistoryService();
        List<HistoricActivityInstance> activities =
                historyService.createHistoricActivityInstanceQuery()
                        .processInstanceId(processInstance.getId())
                        .finished()
                        .orderByHistoricActivityInstanceEndTime().asc()
                        .list();

        for (HistoricActivityInstance activity : activities) {
            System.out.println(activity.getActivityId() + " took "
                    + activity.getDurationInMillis() + " milliseconds");
        }*/
    }
}
