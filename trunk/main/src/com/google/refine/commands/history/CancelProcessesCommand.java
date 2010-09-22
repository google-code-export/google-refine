package com.google.refine.commands.history;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.refine.commands.Command;
import com.google.refine.model.Project;

public class CancelProcessesCommand extends Command {

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if( request == null ) throw new IllegalArgumentException("parameter 'request' should not be null");
        if( response == null ) throw new IllegalArgumentException("parameter 'request' should not be null");

        try {
            Project project = getProject(request);
            project.getProcessManager().cancelAll();

            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");
            response.getWriter().write("{ \"code\" : \"ok\" }");
        } catch (Exception e) {
            respondException(response, e);
        }
    }
}
