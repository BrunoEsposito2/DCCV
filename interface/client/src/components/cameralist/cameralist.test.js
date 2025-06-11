import React from "react";
import { shallow } from "enzyme";
import Cameralist from "./cameralist";

describe("Cameralist", () => {
  test("matches snapshot", () => {
    const wrapper = shallow(<Cameralist />);
    expect(wrapper).toMatchSnapshot();
  });
});
