package com.example.flowable.holiday;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

public class SendRejectionMail implements JavaDelegate {

    @Override
    public void execute(DelegateExecution delegateExecution) {
        System.out.println("send e-mail to employee "
                + delegateExecution.getVariable("employee"));
    }
}
