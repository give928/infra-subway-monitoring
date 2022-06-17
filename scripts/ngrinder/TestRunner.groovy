import groovy.json.JsonSlurper
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.HttpStatus

import static net.grinder.script.Grinder.grinder
import static org.junit.Assert.*

import net.grinder.script.GTest
import net.grinder.scriptengine.groovy.junit.GrinderRunner
import net.grinder.scriptengine.groovy.junit.annotation.BeforeProcess
import net.grinder.scriptengine.groovy.junit.annotation.BeforeThread
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import org.ngrinder.http.HTTPRequest
import org.ngrinder.http.HTTPRequestControl
import org.ngrinder.http.HTTPResponse

@RunWith(GrinderRunner)
class TestRunner {
    public static GTest test
    public static HTTPRequest request
    public static Api api

    @BeforeProcess
    static void beforeProcess() {
        HTTPRequestControl.setConnectionTimeout(300000)
        test = new GTest(1, "test")
        request = new HTTPRequest() // An instance of the plug-in class 'net.grinder.plugin.http.HTTPPlugin' could not be initialised.
        api = new Api()
        grinder.logger.info("before process.")
    }

    @BeforeThread
    void beforeThread() {
        test.record(this, "test")
        grinder.statistics.delayReports = true
        grinder.logger.info("before thread.")
    }

    @Before
    void before() {
        grinder.logger.info("before.")
    }

    @Test
    void test() {
        String uuid = Util.uuid()
        String email = uuid.substring(0, 20) + "@test.com"
        String password = "1234"
        int age = 30

        api.main()

        email = api.join(email, password, age)

        String accessToken = api.login(email, password)

        List favorites = api.favorites(accessToken)

        Map path = api.path(113L, 173L)
    }

    static class Api {
        private static final String URL = "https://give928.kro.kr"

        Api() {
        }

        void main() {
            HTTPResponse response = Util.get(URL)
            assertEquals(response.statusCode, HttpStatus.SC_OK)
        }

        String join(String email, String password, int age) {
            HTTPResponse response = Util.post(URL + "/members", ["email": email, "password": password, "age": age])
            if (response.statusCode != HttpStatus.SC_CREATED) {
                return join(Util.uuid().substring(0, 20) + "@test.com", password, age)
            }
            assertEquals(response.statusCode, HttpStatus.SC_CREATED)
            return email
        }

        String login(String email, String password) {
            HTTPResponse response = Util.post(URL + "/login/token", ["email": email, "password": password])
            assertEquals(response.statusCode, HttpStatus.SC_OK)
            return response.getBody(it -> new JsonSlurper().parseText(it)).accessToken
        }

        List favorites(String accessToken) {
            HTTPResponse response = Util.get(URL + "/favorites", accessToken)
            assertEquals(response.statusCode, HttpStatus.SC_OK)
            return response.getBody(it -> new JsonSlurper().parseText(it)) as List
        }

        Map path(Long source, Long target) {
            HTTPResponse response = Util.get(String.format(URL + "/paths?source=%d&target=%d", source, target))
            assertEquals(response.statusCode, HttpStatus.SC_OK)
            return response.getBody(it -> new JsonSlurper().parseText(it)) as Map
        }
    }

    static class Util {
        private Util() {
        }

        static String uuid() {
            UUID.randomUUID().toString().replaceAll("-", "")
        }

        static HTTPResponse get(String path) {
            return get(path, null)
        }

        static HTTPResponse get(String path, String accessToken) {
            request.setHeaders(header(accessToken))
            return request.GET(path)
        }

        static HTTPResponse post(String path, Map<String, Object> params) {
            request.setHeaders(header())
            return request.POST(path, params, Collections.emptyList())
        }

        static LinkedHashMap<String, String> header() {
            return header(null)
        }

        static LinkedHashMap<String, String> header(String accessToken) {
            Map<String, String> header = ["Content-Type": ContentType.APPLICATION_JSON.toString()]
            if (accessToken != null) {
                header.put("Authorization", "Bearer " + accessToken)
            }
            return header
        }
    }
}
