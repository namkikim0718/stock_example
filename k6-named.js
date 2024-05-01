import http from "k6/http";
import { check } from "k6";

export const options = {
    scenarios: {
        constant_request_rate: {
            executor: 'constant-arrival-rate',
            rate: 30,
            timeUnit: '1s',
            duration: '30s',
            preAllocatedVUs: 2,
            maxVUs: 50
        },
    },
};

export default function () {
    let res = http.post("http://localhost:8080/namedLock/1");
    check(res, {
        "is status 200": (r) => r.status === 200,
    });
}