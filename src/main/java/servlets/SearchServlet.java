package servlets;

import DTO.Function;
import DTO.Point;
import DTO.User;
import JDBC.repository.FunctionRepository;
import JDBC.repository.PointRepository;
import JDBC.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Objects;

public class SearchServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(SearchServlet.class);
    private UserRepository userRepository;
    private FunctionRepository functionRepository;
    private PointRepository pointRepository;
    private ObjectMapper objectMapper;

    @Override
    public void init() {
        this.userRepository = new UserRepository();
        this.functionRepository = new FunctionRepository();
        this.pointRepository = new PointRepository();
        this.objectMapper = new ObjectMapper();
    }

    private void sendError(HttpServletResponse resp, int status, String message) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        ObjectNode error = objectMapper.createObjectNode();
        error.put("error", message);
        PrintWriter out = resp.getWriter();
        out.print(objectMapper.writeValueAsString(error));
        out.flush();
    }

    private User authenticate(HttpServletRequest req) {
        return ServletHelper.authenticateUser(req, userRepository);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        User authUser = authenticate(req);
        
        if (authUser == null) {
            sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
            return;
        }

        if (pathInfo == null || "/".equals(pathInfo)) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid search path");
            return;
        }

        // Remove leading '/'
        pathInfo = pathInfo.substring(1);

        // /api/search/users/username/{username}
        if (pathInfo.startsWith("users/username/")) {
            String username = pathInfo.substring("users/username/".length());
            User user = userRepository.findByUsername(username);
            if (user == null) {
                sendError(resp, HttpServletResponse.SC_NOT_FOUND, "User not found");
                return;
            }
            // Access check: can get own data or be ADMIN
            if (!username.equals(authUser.getUsername()) && !"ADMIN".equals(authUser.getRole())) {
                sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Access denied");
                return;
            }
            user.setPasswordHash(null);
            resp.setContentType("application/json");
            resp.getWriter().print(objectMapper.writeValueAsString(user));
            logger.info("Search: User by username {} returned", username);
            return;
        }

        // /api/search/functions/{functionId}
        if (pathInfo.startsWith("functions/") && !pathInfo.contains("/user/")) {
            String rest = pathInfo.substring("functions/".length());
            // Check if it's just a number (functionId) or has more path
            if (!rest.contains("/")) {
                try {
                    int functionId = Integer.parseInt(rest);
                    Function function = functionRepository.findById(functionId);
                    if (function == null) {
                        sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Function not found");
                        return;
                    }
                    // Access check: owner or ADMIN
                    if (!Objects.equals(function.getUserId(), authUser.getId()) && !"ADMIN".equals(authUser.getRole())) {
                        sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Access denied");
                        return;
                    }
                    resp.setContentType("application/json");
                    resp.getWriter().print(objectMapper.writeValueAsString(function));
                    logger.info("Search: Function by id {} returned", functionId);
                    return;
                } catch (NumberFormatException e) {
                    sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid function ID");
                    return;
                }
            }
        }

        // /api/search/functions/user/{userId}
        if (pathInfo.startsWith("functions/user/")) {
            String rest = pathInfo.substring("functions/user/".length());
            // Check if there are query parameters (nameLike, sortBy, sortDir)
            String userIdStr = rest;
            if (rest.contains("?")) {
                userIdStr = rest.substring(0, rest.indexOf("?"));
            }
            try {
                int userId = Integer.parseInt(userIdStr);
                if (userId != authUser.getId() && !"ADMIN".equals(authUser.getRole())) {
                    sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Access denied");
                    return;
                }
                
                List<Function> functions = functionRepository.findByUserId(userId);
                
                // Filter by nameLike if provided
                String nameLike = req.getParameter("nameLike");
                if (nameLike != null && !nameLike.trim().isEmpty()) {
                    String nameFilter = nameLike.toLowerCase();
                    functions = functions.stream()
                        .filter(f -> f.getName() != null && f.getName().toLowerCase().contains(nameFilter))
                        .collect(java.util.stream.Collectors.toList());
                }
                
                // Sort
                String sortBy = req.getParameter("sortBy");
                if (sortBy == null || sortBy.isEmpty()) {
                    sortBy = "id";
                }
                String sortDir = req.getParameter("sortDir");
                if (sortDir == null || sortDir.isEmpty()) {
                    sortDir = "asc";
                }
                
                final String finalSortBy = sortBy;
                final boolean ascending = !"desc".equalsIgnoreCase(sortDir);
                functions.sort((f1, f2) -> {
                    int result = 0;
                    switch (finalSortBy) {
                        case "name":
                            String n1 = f1.getName() != null ? f1.getName() : "";
                            String n2 = f2.getName() != null ? f2.getName() : "";
                            result = n1.compareToIgnoreCase(n2);
                            break;
                        case "id":
                        default:
                            result = Integer.compare(f1.getId() != null ? f1.getId() : 0, 
                                                   f2.getId() != null ? f2.getId() : 0);
                            break;
                    }
                    return ascending ? result : -result;
                });
                
                resp.setContentType("application/json");
                resp.getWriter().print(objectMapper.writeValueAsString(functions));
                logger.info("Search: Functions by user {} returned", userId);
                return;
            } catch (NumberFormatException e) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid userId");
                return;
            }
        }

        // /api/search/points/function/{functionId}/x/{xValue}
        if (pathInfo.startsWith("points/function/") && pathInfo.contains("/x/")) {
            String[] parts = pathInfo.split("/x/");
            if (parts.length == 2) {
                try {
                    int functionId = Integer.parseInt(parts[0].substring("points/function/".length()));
                    double xValue = Double.parseDouble(parts[1]);
                    
                    Function function = functionRepository.findById(functionId);
                    if (function == null) {
                        sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Function not found");
                        return;
                    }
                    // Access check: owner or ADMIN
                    if (!Objects.equals(function.getUserId(), authUser.getId()) && !"ADMIN".equals(authUser.getRole())) {
                        sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Access denied");
                        return;
                    }
                    
                    Point point = pointRepository.findByFunctionIdAndX(functionId, xValue);
                    if (point == null) {
                        sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Point not found");
                        return;
                    }
                    resp.setContentType("application/json");
                    resp.getWriter().print(objectMapper.writeValueAsString(point));
                    logger.info("Search: Point by function {} and x {} returned", functionId, xValue);
                    return;
                } catch (NumberFormatException e) {
                    sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid functionId or xValue");
                    return;
                }
            }
        }

        // /api/search/points/function/{functionId}
        if (pathInfo.startsWith("points/function/") && !pathInfo.contains("/x/")) {
            String rest = pathInfo.substring("points/function/".length());
            String functionIdStr = rest;
            if (rest.contains("?")) {
                functionIdStr = rest.substring(0, rest.indexOf("?"));
            }
            try {
                int functionId = Integer.parseInt(functionIdStr);
                
                Function function = functionRepository.findById(functionId);
                if (function == null) {
                    sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Function not found");
                    return;
                }
                // Access check: owner or ADMIN
                if (!Objects.equals(function.getUserId(), authUser.getId()) && !"ADMIN".equals(authUser.getRole())) {
                    sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Access denied");
                    return;
                }
                
                List<Point> points = pointRepository.findByFunctionId(functionId);
                
                // Filter by range if provided
                String fromXParam = req.getParameter("fromX");
                String toXParam = req.getParameter("toX");
                if (fromXParam != null || toXParam != null) {
                    double fromX = fromXParam != null ? Double.parseDouble(fromXParam) : Double.NEGATIVE_INFINITY;
                    double toX = toXParam != null ? Double.parseDouble(toXParam) : Double.POSITIVE_INFINITY;
                    points = points.stream()
                        .filter(p -> p.getXValue() >= fromX && p.getXValue() <= toX)
                        .collect(java.util.stream.Collectors.toList());
                }
                
                // Sort
                String sortBy = req.getParameter("sortBy");
                String sortDir = req.getParameter("sortDir");
                if (sortBy != null && !sortBy.isEmpty()) {
                    final boolean ascending = !"desc".equalsIgnoreCase(sortDir);
                    if (sortBy.contains("xValue") || sortBy.contains("x")) {
                        points.sort((p1, p2) -> {
                            int result = Double.compare(p1.getXValue(), p2.getXValue());
                            return ascending ? result : -result;
                        });
                    } else if (sortBy.contains("yValue") || sortBy.contains("y")) {
                        points.sort((p1, p2) -> {
                            int result = Double.compare(p1.getYValue(), p2.getYValue());
                            return ascending ? result : -result;
                        });
                    }
                }
                
                resp.setContentType("application/json");
                resp.getWriter().print(objectMapper.writeValueAsString(points));
                logger.info("Search: Points by function {} returned", functionId);
                return;
            } catch (NumberFormatException e) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid functionId");
                return;
            }
        }

        // /api/search/hierarchy/user/{userId}/breadth-first
        if (pathInfo.startsWith("hierarchy/user/") && pathInfo.contains("/breadth-first")) {
            String userIdStr = pathInfo.substring("hierarchy/user/".length(), pathInfo.indexOf("/breadth-first"));
            try {
                int userId = Integer.parseInt(userIdStr);
                if (userId != authUser.getId() && !"ADMIN".equals(authUser.getRole())) {
                    sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Access denied");
                    return;
                }
                // TODO: Implement breadth-first search
                resp.setContentType("application/json");
                resp.getWriter().print("[]");
                logger.info("Search: Hierarchy breadth-first for user {} (not implemented)", userId);
                return;
            } catch (NumberFormatException e) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid userId");
                return;
            }
        }

        // /api/search/hierarchy/user/{userId}/depth-first
        if (pathInfo.startsWith("hierarchy/user/") && pathInfo.contains("/depth-first")) {
            String userIdStr = pathInfo.substring("hierarchy/user/".length(), pathInfo.indexOf("/depth-first"));
            try {
                int userId = Integer.parseInt(userIdStr);
                if (userId != authUser.getId() && !"ADMIN".equals(authUser.getRole())) {
                    sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Access denied");
                    return;
                }
                // TODO: Implement depth-first search
                resp.setContentType("application/json");
                resp.getWriter().print("[]");
                logger.info("Search: Hierarchy depth-first for user {} (not implemented)", userId);
                return;
            } catch (NumberFormatException e) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid userId");
                return;
            }
        }

        sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid search path");
    }
}

