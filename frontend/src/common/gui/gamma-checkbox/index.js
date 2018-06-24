import React from "react";

import PropTypes from "prop-types";

import { FormControlLabel, Checkbox } from "@material-ui/core";
import { getColor } from "../common/utils/color";

export class GammaCheckbox extends React.Component {
  state = {
    checked: this.props.startValue != null ? this.props.startValue : false
  };

  render() {
    return (
      <FormControlLabel
        label={this.props.label}
        disabled={this.props.disabled}
        control={
          <Checkbox
            color={getColor(this.props.primary, this.props.secondary)}
            checked={this.state.checked}
            onChange={e => {
              const checked = e.target.checked;
              this.setState({
                ...this.state,
                checked: checked
              });

              this.props.onChange(checked);
            }}
          />
        }
      />
    );
  }
}

GammaCheckbox.propTypes = {
  onChange: PropTypes.func.isRequired,
  label: PropTypes.string.isRequired,
  startValue: PropTypes.bool,
  primary: PropTypes.bool,
  secondary: PropTypes.bool,
  disabled: PropTypes.bool
};
