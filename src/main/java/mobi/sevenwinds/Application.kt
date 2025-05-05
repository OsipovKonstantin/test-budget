package mobi.sevenwinds

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import com.papsign.ktor.openapigen.annotations.type.common.ConstraintViolation
import com.papsign.ktor.openapigen.exceptions.OpenAPIRequiredFieldException
import com.papsign.ktor.openapigen.route.apiRouting
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.locations.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.netty.*
import mobi.sevenwinds.app.Config
import mobi.sevenwinds.modules.DatabaseFactory
import mobi.sevenwinds.modules.initSwagger
import mobi.sevenwinds.modules.serviceRouting
import mobi.sevenwinds.modules.swaggerRouting
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.text.SimpleDateFormat

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    Config.init(environment.config)
    DatabaseFactory.init(environment.config)

    initSwagger()

    install(DefaultHeaders)

    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            registerModule(Jdk8Module())
            registerModule(SimpleModule().apply {
                addSerializer(DateTime::class.java, object : JsonSerializer<DateTime>() {
                    private val formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")
                    override fun serialize(value: DateTime?, gen: JsonGenerator, serializers: SerializerProvider) {
                        gen.writeString(value?.toString(formatter))
                    }
                })
            })
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }

    install(Locations) {
    }

    install(CallLogging) {
        level = Level.INFO
        filter { call ->
            Config.logAllRequests ||
            call.request.path().startsWith("/")
                    && (call.response.status()?.value ?: 0) >= 500
        }
    }

    install(CORS) {
        method(HttpMethod.Options)
        method(HttpMethod.Put)
        method(HttpMethod.Delete)
        method(HttpMethod.Patch)
        header(HttpHeaders.Authorization)
        header(HttpHeaders.ContentType)
        header(HttpHeaders.AccessControlAllowOrigin)
        allowCredentials = true
        allowNonSimpleContentTypes = true
        allowSameOrigin = true
        anyHost()
    }

    apiRouting {
        swaggerRouting()
    }

    routing {
        serviceRouting()
    }

    install(StatusPages) {
        val log = LoggerFactory.getLogger("InternalError")

        exception<NotFoundException> { cause ->
            call.respond(HttpStatusCode.NotFound, cause.message ?: "")
        }
        exception<OpenAPIRequiredFieldException> { cause ->
            call.respond(HttpStatusCode.BadRequest, cause.message ?: "")
        }
        exception<MissingKotlinParameterException> { cause ->
            call.respond(HttpStatusCode.BadRequest, cause.message ?: "")
        }
        exception<ConstraintViolation> { cause ->
            call.respond(HttpStatusCode.BadRequest, cause.message ?: "")
        }
        exception<IllegalArgumentException> { cause ->
            call.respond(HttpStatusCode.BadRequest, cause.message ?: "")
            cause.printStackTrace()
            log.error("", cause)
        }
        exception<Throwable> { cause ->
            call.respond(HttpStatusCode.InternalServerError, cause.message ?: "")
            cause.printStackTrace()
            log.error("", cause)
        }
    }
}