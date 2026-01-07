package functions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Математическая функция, вычисляемая из строкового выражения
 * Поддерживает основные математические функции: sin, cos, tan, log, exp, sqrt и т.д.
 */
public class ExpressionMathFunction implements MathFunction {
    private static final Logger logger = LoggerFactory.getLogger(ExpressionMathFunction.class);
    private final String expression;

    public ExpressionMathFunction(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            throw new IllegalArgumentException("Выражение не может быть пустым");
        }
        this.expression = expression.trim();
    }

    @Override
    public double apply(double x) {
        try {
            String expr = expression;
            
            // Сначала обрабатываем степень (до замены x), так как x может быть в степени
            // Заменяем x^число на Math.pow(x, число) перед заменой x
            expr = processPowerWithX(expr, x);
            
            // Заменяем оставшиеся x на значение (с учетом границ слова, чтобы не заменять в функциях типа exp, log)
            expr = expr.replaceAll("\\bx\\b", String.valueOf(x));
            
            // Обрабатываем математические функции
            expr = processMathFunctions(expr);
            
            // Заменяем оставшиеся ^ на возведение в степень через Math.pow (для чисел)
            expr = processPower(expr);
            
            // Вычисляем выражение
            return evaluateExpression(expr);
        } catch (Exception e) {
            logger.error("Ошибка вычисления выражения '{}' для x={}: {}", expression, x, e.getMessage());
            throw new IllegalArgumentException("Ошибка вычисления выражения: " + e.getMessage());
        }
    }
    
    private String processPowerWithX(String expr, double x) {
        // Обрабатываем x^число и число^x
        // x^число
        Pattern pattern1 = Pattern.compile("\\bx\\s*\\^\\s*([0-9.]+)");
        Matcher matcher1 = pattern1.matcher(expr);
        StringBuffer result1 = new StringBuffer();
        while (matcher1.find()) {
            double exp = Double.parseDouble(matcher1.group(1));
            double powResult = Math.pow(x, exp);
            matcher1.appendReplacement(result1, String.valueOf(powResult));
        }
        matcher1.appendTail(result1);
        expr = result1.toString();
        
        // число^x
        Pattern pattern2 = Pattern.compile("([0-9.]+)\\s*\\^\\s*\\bx\\b");
        Matcher matcher2 = pattern2.matcher(expr);
        StringBuffer result2 = new StringBuffer();
        while (matcher2.find()) {
            double base = Double.parseDouble(matcher2.group(1));
            double powResult = Math.pow(base, x);
            matcher2.appendReplacement(result2, String.valueOf(powResult));
        }
        matcher2.appendTail(result2);
        expr = result2.toString();
        
        // x^x
        Pattern pattern3 = Pattern.compile("\\bx\\s*\\^\\s*\\bx\\b");
        Matcher matcher3 = pattern3.matcher(expr);
        StringBuffer result3 = new StringBuffer();
        while (matcher3.find()) {
            double powResult = Math.pow(x, x);
            matcher3.appendReplacement(result3, String.valueOf(powResult));
        }
        matcher3.appendTail(result3);
        expr = result3.toString();
        
        return expr;
    }

    private String processMathFunctions(String expr) {
        // Обрабатываем функции с одним аргументом
        String[] singleArgFunctions = {"sin", "cos", "tan", "asin", "acos", "atan", 
                                       "sinh", "cosh", "tanh", "exp", "log", "log10", 
                                       "sqrt", "abs", "ceil", "floor", "round"};
        
        for (String func : singleArgFunctions) {
            Pattern pattern = Pattern.compile("\\b" + func + "\\s*\\(");
            Matcher matcher = pattern.matcher(expr);
            StringBuffer result = new StringBuffer();
            
            while (matcher.find()) {
                int start = matcher.end();
                int depth = 1;
                int end = start;
                
                // Находим закрывающую скобку
                while (end < expr.length() && depth > 0) {
                    if (expr.charAt(end) == '(') depth++;
                    if (expr.charAt(end) == ')') depth--;
                    end++;
                }
                
                if (depth == 0) {
                    String arg = expr.substring(start, end - 1);
                    double argValue = evaluateExpression(arg);
                    double resultValue = applyMathFunction(func, argValue);
                    matcher.appendReplacement(result, String.valueOf(resultValue));
                } else {
                    break; // Неправильные скобки
                }
            }
            matcher.appendTail(result);
            expr = result.toString();
        }
        
        // Обрабатываем pow(x, y)
        Pattern powPattern = Pattern.compile("\\bpow\\s*\\(");
        Matcher powMatcher = powPattern.matcher(expr);
        StringBuffer powResult = new StringBuffer();
        
        while (powMatcher.find()) {
            int start = powMatcher.end();
            int depth = 1;
            int end = start;
            int commaPos = -1;
            
            // Находим запятую и закрывающую скобку
            while (end < expr.length() && depth > 0) {
                if (expr.charAt(end) == '(') depth++;
                if (expr.charAt(end) == ')') depth--;
                if (depth == 1 && expr.charAt(end) == ',' && commaPos == -1) {
                    commaPos = end;
                }
                end++;
            }
            
            if (depth == 0 && commaPos != -1) {
                String base = expr.substring(start, commaPos);
                String exp = expr.substring(commaPos + 1, end - 1);
                double baseValue = evaluateExpression(base);
                double expValue = evaluateExpression(exp);
                double resultValue = Math.pow(baseValue, expValue);
                powMatcher.appendReplacement(powResult, String.valueOf(resultValue));
            } else {
                break;
            }
        }
        powMatcher.appendTail(powResult);
        expr = powResult.toString();
        
        return expr;
    }

    private String processPower(String expr) {
        // Обрабатываем оператор ^ (степень)
        Pattern pattern = Pattern.compile("([0-9.]+|\\.[0-9]+|[0-9]+\\.[0-9]+)\\s*\\^\\s*([0-9.]+|\\.[0-9]+|[0-9]+\\.[0-9]+)");
        Matcher matcher = pattern.matcher(expr);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            double base = Double.parseDouble(matcher.group(1));
            double exp = Double.parseDouble(matcher.group(2));
            double powResult = Math.pow(base, exp);
            matcher.appendReplacement(result, String.valueOf(powResult));
        }
        matcher.appendTail(result);
        
        return result.toString();
    }

    private double applyMathFunction(String funcName, double arg) {
        return switch (funcName) {
            case "sin" -> Math.sin(arg);
            case "cos" -> Math.cos(arg);
            case "tan" -> Math.tan(arg);
            case "asin" -> Math.asin(arg);
            case "acos" -> Math.acos(arg);
            case "atan" -> Math.atan(arg);
            case "sinh" -> Math.sinh(arg);
            case "cosh" -> Math.cosh(arg);
            case "tanh" -> Math.tanh(arg);
            case "exp" -> Math.exp(arg);
            case "log" -> Math.log(arg);
            case "log10" -> Math.log10(arg);
            case "sqrt" -> Math.sqrt(arg);
            case "abs" -> Math.abs(arg);
            case "ceil" -> Math.ceil(arg);
            case "floor" -> Math.floor(arg);
            case "round" -> Math.round(arg);
            default -> throw new IllegalArgumentException("Неизвестная функция: " + funcName);
        };
    }

    private double evaluateExpression(String expr) {
        expr = expr.trim();
        
        // Если это просто число
        if (expr.matches("-?[0-9]+\\.?[0-9]*")) {
            return Double.parseDouble(expr);
        }
        
        // Обрабатываем скобки
        while (expr.contains("(")) {
            int openIndex = expr.lastIndexOf('(');
            int closeIndex = expr.indexOf(')', openIndex);
            if (closeIndex == -1) {
                throw new IllegalArgumentException("Незакрытые скобки в выражении");
            }
            
            String inner = expr.substring(openIndex + 1, closeIndex);
            double innerValue = evaluateExpression(inner);
            expr = expr.substring(0, openIndex) + innerValue + expr.substring(closeIndex + 1);
        }
        
        // Обрабатываем операции по приоритету
        // Умножение и деление
        expr = processOperations(expr, new String[]{"*", "/"});
        
        // Сложение и вычитание
        expr = processOperations(expr, new String[]{"+", "-"});
        
        return Double.parseDouble(expr.trim());
    }

    private String processOperations(String expr, String[] operators) {
        for (String op : operators) {
            Pattern pattern = Pattern.compile("(-?[0-9.]+)\\s*\\" + op + "\\s*(-?[0-9.]+)");
            Matcher matcher = pattern.matcher(expr);
            StringBuffer result = new StringBuffer();
            
            while (matcher.find()) {
                double left = Double.parseDouble(matcher.group(1));
                double right = Double.parseDouble(matcher.group(2));
                double resultValue = switch (op) {
                    case "*" -> left * right;
                    case "/" -> {
                        if (right == 0) throw new ArithmeticException("Деление на ноль");
                        yield left / right;
                    }
                    case "+" -> left + right;
                    case "-" -> left - right;
                    default -> throw new IllegalArgumentException("Неизвестная операция: " + op);
                };
                matcher.appendReplacement(result, String.valueOf(resultValue));
            }
            matcher.appendTail(result);
            expr = result.toString();
        }
        
        return expr;
    }

    public String getExpression() {
        return expression;
    }

    /**
     * Валидирует выражение, пытаясь вычислить его для тестового значения
     */
    public static boolean isValidExpression(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return false;
        }
        
        try {
            ExpressionMathFunction func = new ExpressionMathFunction(expression);
            // Пробуем вычислить для нескольких тестовых значений
            func.apply(0.0);
            func.apply(1.0);
            func.apply(-1.0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

