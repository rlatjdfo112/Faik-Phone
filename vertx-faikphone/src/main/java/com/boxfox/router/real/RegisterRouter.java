package com.boxfox.router.real;

import com.boxfox.additional.MessageManager;
import com.boxfox.dao.ChangeDAO;
import com.boxfox.support.handler.AbstractDAHandler;
import com.boxfox.support.routing.Route;
import com.boxfox.support.utilities.response.ResponseJsonUtil;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import org.json.JSONObject;

/**
 * Created by boxfox on 2017-07-13.
 */
@Route(uri = "/real.do/register", method = HttpMethod.POST)
public class RegisterRouter extends AbstractDAHandler<ChangeDAO> {
    public RegisterRouter() {
        super(ChangeDAO.class);
    }

    @Override
    public void handle(RoutingContext ctx, ChangeDAO dao) {
        HttpServerRequest request = ctx.request();
        HttpServerResponse response = ctx.response();
        if (dao.insertRealPhoneToken(request.getParam("token"), request.getParam("pnum"))) {
            response.setStatusCode(200);
            response.end(dao.getAuthCode(request.getParam("token")));
        } else {
            response.setStatusCode(400);
        }
        if(!response.ended())
            response.end();
        response.close();
    }

}
