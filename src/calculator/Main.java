package calculator;

import java.math.BigInteger;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    String rxNumber = "\\d+|^-?\\d+";
    String rxOperator = "[\\^*\\-+/=]";
    String rxParentheses = "[()]";
    String rxVariable = "\\w+";
    String rxExpression = rxNumber + "|" + rxOperator + "|" + rxVariable + "|" + rxParentheses;
    Pattern p = Pattern.compile(rxExpression);
    private final Scanner scanner;
    private final List<String> postfix = new ArrayList<>();
    private final HashMap<String, BigInteger> variableStorage = new HashMap<String, BigInteger>();
    private final HashMap<String, Long> precedenceStorage = new HashMap<String, Long>();
    private int logLevel = 0;

    public Main(Scanner scanner) {
        this.scanner = scanner;
        InitPrecedenceStorage();
    }

    public static void main(String[] args) {
        Main main = new Main(new Scanner(System.in));
        main.run();
    }

    private void run() {
        // put your code here
        boolean cont = true;
        do {
            String s1 = scanner.nextLine();
            if ("/help".equals(s1)) {
                System.out.println("The program evaluates the expression.");
            } else if ("/exit".equals(s1)) {
                cont = false;
            } else if (s1.startsWith("/loglevel:")) {
                Pattern p = Pattern.compile("\\d+");
                Matcher m = p.matcher(s1);
                if (m.find()) {
                    String t = m.group();
                    int logLevel = Integer.parseInt(t);
                    logger("Setting loglevel to " + logLevel);
                    setLogLevel(logLevel);
                }
            } else if (s1.startsWith("/")) {
                System.out.println("Unknown command");
            } else if ("".equals(s1)) {
                // do nothing
            } else {
                try {
                    handleExpression(s1);
                } catch (Exception e) {
                    System.out.println("Invalid expression");
                }
            }
        } while (cont);
        System.out.println("Bye!");
    }

    private void showError(String expression, String message) {
        System.out.println("Der Ausdruck " + expression + " konnte nicht verarbeitet werden.");
        System.out.println("Die Fehlermeldung lautet: ");
        System.out.println(message);
    }

    private void setLogLevel(int logLevel) {
        this.logLevel = logLevel;
    }

    private void InitPrecedenceStorage() {
        precedenceStorage.clear();
        precedenceStorage.put("+", 2L);
        precedenceStorage.put("-", 2L);
        precedenceStorage.put("*", 3L);
        precedenceStorage.put("/", 3L);
        precedenceStorage.put("^", 4L);
    }

    private void handleExpression(String expression) {

        expression = prepareExpression(expression);
        infix2postfix(expression);
        BigInteger result = calcResult();
        printResult(result);
    }

    private String prepareExpression(String expression) {

        expression = expression.replaceAll("\\++", "+");
        expression = expression.replaceAll("--", "+");

        logger("Homogenized expression: " + expression);
        return expression;
    }

    private void printResult(BigInteger result) {
        if (result != null) {
            System.out.println(result);
        }
    }

    private String getVariableValue(String varnameOrValue) {
        if (variableStorage.containsKey(varnameOrValue)) {
            return variableStorage.get(varnameOrValue).toString();
        } else {
            return varnameOrValue;
        }
    }

    private BigInteger calcResult() {
        Stack<String> stack = new Stack<>();
        boolean wasAssignment = false;

        for (String p : postfix) {

            if (p.matches(rxNumber)) {
                logger("push Number to stack: " + p);
                stack.push(p);
            } else if (p.matches(rxVariable)) {
                stack.push(p);
            } else if (p.matches(rxOperator)) {
                logger("got operator: " + p);
                String s1 = stack.pop();
                String s2 = stack.pop();
                logger("values: " + s2 + ", " + s1);

                if (!"=".equals(p)) {
                    s2 = getVariableValue(s2);
                }
                s1 = getVariableValue(s1);

                BigInteger res = Operation(p, s2, s1);
                String r = res.toString();
                logger("push result of " + s2 + " " + p + " " + s1 + " = " + res + " to stack");
                stack.push(r);

                if ("=".equals(p)) {
                    wasAssignment = true;
                }
            }
        }

        String result = stack.pop();
        result = getVariableValue(result);
        if (wasAssignment) {
            return null;
        } else {
            return new BigInteger(result);
        }
    }

    private BigInteger Operation(String op, String s1, String s2) {
        switch (op) {
            case "=":
                BigInteger v = new BigInteger(s2);
                if (variableStorage.containsKey(s1)) {
                    variableStorage.replace(s1, v);
                } else {
                    variableStorage.put(s1, v);
                }
                return v;
            case "+":
                BigInteger v1 = new BigInteger(s1);
                BigInteger v2 = new BigInteger(s2);
                return v1.add(v2);
            case "-":
                v1 = new BigInteger(s1);
                v2 = new BigInteger(s2);
                return v1.subtract(v2);
            case "/":
                v1 = new BigInteger(s1);
                v2 = new BigInteger(s2);
                return v1.divide(v2);
            case "*":
                v1 = new BigInteger(s1);
                v2 = new BigInteger(s2);
                return v1.multiply(v2);

            case "^":
                v1 = new BigInteger(s1);
                int i2 = Integer.parseInt(s2);
                return v1.pow(i2);

        }
        return null;
    }

    private void infix2postfix(String expression) {

        checkParentheses(expression);

        Stack<String> stack = new Stack<>();
        Matcher m = p.matcher(expression);
        postfix.clear();
        while (m.find()) {
            String g = m.group();
            if (g.matches(rxNumber) || g.matches(rxVariable)) {
                // [1] Add operands (numbers and variables) to the result (postfix notation) as they arrive.
                logger("[1] Add operands (numbers and variables) to the result (postfix notation) as they arrive.");
                postfix.add(g);
            } else if (g.matches(rxOperator)) {

                // [2] If the stack is empty or contains a left parenthesis on top, push the incoming operator on the stack.
                if (stack.isEmpty() || Objects.equals(stack.lastElement(), "(")) {
                    logger("[2] If the stack is empty or contains a left parenthesis on top, push the incoming operator on the stack.");
                    stack.push(g);
                }
                // [3] If the incoming operator has higher precedence than the top of the stack, push it on the stack.
                else if (getPrecedenceOf(g) > getPrecedenceOf(stack.lastElement())) {
                    logger("[3] If the incoming operator has higher precedence than the top of the stack, push it on the stack.");
                    stack.push(g);
                }

                // [4] If the incoming operator has lower or equal precedence than the top of the operator stack,
                // pop the stack and add operators to the result until you see an operator that has a smaller precedence
                // or a left parenthesis on the top of the stack; then add the incoming operator to the stack.

                else if (getPrecedenceOf(g) <= getPrecedenceOf(stack.lastElement())) {
                    logger("[4] if the incoming operator has lower or equal precedence than the top of the operator stack");
                    logger("    pop the stack and add operators to the result until you see an operator that has a smaller precedence ");
                    logger("    or a left parenthesis on the top of the stack; then add the incoming operator to the stack.");
                    Long topPrecedence = getPrecedenceOf(stack.lastElement());
                    do {
                        if (stack.isEmpty()) {
                            break;
                        }
                        String op = stack.pop();
                        logger("   pop (" + op + ") and add to result");
                        postfix.add(op);
                        if ("(".equals(op) || getPrecedenceOf(op) < topPrecedence) {
                            break;
                        }


                    } while (true);
                    stack.push(g);
                }
            } else if (g.matches(rxParentheses)) {
                // [5] if the incoming element is a left parenthesis, push it on the stack.
                if ("(".equals(g)) {
                    logger("[5] if the incoming element is a left parenthesis, push it on the stack.");
                    stack.push(g);
                }

                // [6] if the incoming element is a right parenthesis, pop the stack and add operators to the result
                // until you see a left parenthesis. Discard the pair of parentheses.
                else if (")".equals(g)) {
                    logger("[6] if the incoming element is a right parenthesis, pop the stack and add operators to the result until you see a left parenthesis. Discard the pair of parentheses.");
                    do {
                        if (stack.isEmpty()) {
                            logger("***   Stack is empty: stoping to add ops to result");
                            //throw new InputMismatchException("Invalid expression. Missing '('.");
                            break;
                        }
                        String op = stack.pop();
                        if ("(".equals(op)) {
                            logger("  found ( stopping to add ops to result");
                            break;
                        }
                        logger("   pop (" + op + ") and add to result");
                        postfix.add(op);
                    } while (true);
                } else {
                    throw new InputMismatchException("Invalid token: " + g);
                }
            }

            logger("stack: " + stack.toString());
            logger("result: " + postfix.toString());
        }
        logger("adding stack to result");
        while (!stack.isEmpty()) {
            String op = stack.pop();
            postfix.add(op);
        }
        logger("result: " + postfix.toString());


    }

    private void checkParentheses(String expression) {
        // Number of parentheses must match
        int numOfPOpen = 0;
        int numOfPClose = 0;

        for (char p : expression.toCharArray()) {
            if (p == '(') {
                numOfPOpen++;
            }
            if (p == ')') {
                numOfPClose++;
            }
        }

        if (numOfPClose != numOfPOpen) {
            throw new InputMismatchException("Parentheses-Mismatch! " + numOfPOpen + "!=" + numOfPClose);
        }
    }

    private void logger(String s) {
        if (this.logLevel > 0) {
            System.out.println(s);
        }
    }

    private Long getPrecedenceOf(String g) {
        return precedenceStorage.getOrDefault(g, 0L);
    }

    private BigInteger getValueOf(String g) {
        return variableStorage.get(g);
    }

}
